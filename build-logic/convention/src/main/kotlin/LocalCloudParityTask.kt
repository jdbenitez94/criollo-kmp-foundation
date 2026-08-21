import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.inject.Inject

/**
 * Optional local DX: markdownlint + jscpd (Codacy-style duplication), then optional Kover XML +
 * best-effort Codecov / Codacy coverage uploads when tokens exist in `local.properties`.
 *
 * Complexity is covered by Detekt (`./gradlew detekt` / `qualityCheck`).
 *
 * Enable coverage path with `-PlocalCloudParity.coverage=true` (or the same key in
 * `local.properties` / `gradle.properties`).
 */
@UntrackedTask(because = "Invokes network CLIs and local tooling outside Gradle inputs.")
abstract class LocalCloudParityTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:Internal
    abstract val rootDirProperty: DirectoryProperty

    @get:Internal
    abstract val codecovSlug: Property<String>

    @get:Internal
    abstract val runCoverageUploads: Property<Boolean>

    @get:Internal
    abstract val failOnMarkdownlint: Property<Boolean>

    @get:Internal
    abstract val failOnJscpd: Property<Boolean>

    @get:Internal
    abstract val koverReportFile: RegularFileProperty

    @TaskAction
    fun run() {
        val rootDir = rootDirProperty.get().asFile
        runMarkdownlint(rootDir)
        runJscpd(rootDir)

        if (!runCoverageUploads.get()) {
            logger.lifecycle(
                "Skipping coverage uploads (pass -PlocalCloudParity.coverage=true to generate " +
                    "koverXmlReport and best-effort Codecov/Codacy uploads).",
            )
            return
        }

        val report = koverReportFile.get().asFile
        if (!report.isFile) {
            logger.warn("Kover XML not found at ${report.path}; skip coverage uploads.")
            return
        }

        val props = loadLocalProperties(rootDir)
        uploadCodecovBestEffort(rootDir, report, props)
        uploadCodacyBestEffort(rootDir, report, props)
    }

    private fun runMarkdownlint(rootDir: java.io.File) {
        logger.lifecycle("Running markdownlint-cli2…")
        val result = execOperations.exec {
            workingDir = rootDir
            isIgnoreExitValue = true
            commandLine(
                "npx",
                "--yes",
                "markdownlint-cli2",
                "**/*.md",
                "#node_modules",
                "#site",
                "#build",
                "#.gradle",
                "#.venv",
                "#iosApp",
            )
        }
        if (result.exitValue != 0) {
            if (failOnMarkdownlint.get()) {
                error(
                    "markdownlint-cli2 failed (exit ${result.exitValue}). " +
                        "Fix Markdown issues or adjust .markdownlint.json.",
                )
            }
            logger.warn(
                "markdownlint-cli2 reported issues (exit ${result.exitValue}); " +
                    "continuing (failOnMarkdownlint=false).",
            )
            return
        }
        logger.lifecycle("markdownlint-cli2 passed.")
    }

    private fun runJscpd(rootDir: java.io.File) {
        logger.lifecycle("Running jscpd (duplication, Codacy-compatible)…")
        val config = rootDir.resolve(".jscpd.json")
        val command = mutableListOf("npx", "--yes", "jscpd", ".")
        if (config.isFile) {
            command += listOf("--config", config.absolutePath)
        } else {
            command += listOf(
                "--format",
                "kotlin",
                "--threshold",
                "5",
                "--ignore",
                "**/build/**,**/.gradle/**,**/node_modules/**,**/.git/**,**/.venv/**,**/site/**,**/iosApp/**",
            )
        }
        // Exit code comes from .jscpd.json `threshold` (percent), not from any-clone --exit-code.
        val result = execOperations.exec {
            workingDir = rootDir
            isIgnoreExitValue = true
            commandLine(command)
        }
        if (result.exitValue != 0) {
            if (failOnJscpd.get()) {
                error(
                    "jscpd found duplicated code above threshold (exit ${result.exitValue}). " +
                        "Refactor clones or adjust .jscpd.json (Codacy uses jscpd for Kotlin).",
                )
            }
            logger.warn(
                "jscpd reported duplication (exit ${result.exitValue}); " +
                    "continuing (failOnJscpd=false). Complexity: ./gradlew detekt",
            )
            return
        }
        logger.lifecycle("jscpd passed (Kotlin duplication within .jscpd.json threshold).")
    }

    private fun uploadCodecovBestEffort(
        rootDir: java.io.File,
        report: java.io.File,
        props: Properties,
    ) {
        val token = firstProp(
            props,
            "codecovRepositoryToken",
            "codecovApiToken",
            "CODECOV_TOKEN",
        )
        if (token == null) {
            logger.lifecycle(
                "Codecov: skipped (set codecovRepositoryToken or codecovApiToken in local.properties).",
            )
            return
        }
        logger.lifecycle("Codecov: uploading ${report.name} (best-effort)…")
        val result = runCatching {
            execQuiet(rootDir) {
                commandLine(
                    "npx",
                    "--yes",
                    "codecov",
                    "--token=$token",
                    "--file=${report.absolutePath}",
                    "--slug=${codecovSlug.get()}",
                    "--nonZero",
                )
            }
        }
        result.fold(
            onSuccess = { exec ->
                if (exec.exitValue == 0) {
                    logger.lifecycle("Codecov: upload OK.")
                } else {
                    logger.warn("Codecov: upload failed (exit ${exec.exitValue}); continuing.")
                }
            },
            onFailure = { e ->
                logger.warn("Codecov: upload error (${e.message}); continuing.")
            },
        )
    }

    private fun uploadCodacyBestEffort(
        rootDir: java.io.File,
        report: java.io.File,
        props: Properties,
    ) {
        val apiToken = firstProp(props, "codacyApiToken", "codacyToken", "CODACY_API_TOKEN")
        val projectToken = firstProp(props, "codacyProjectToken", "CODACY_PROJECT_TOKEN")
        if (apiToken == null && projectToken == null) {
            logger.lifecycle(
                "Codacy: skipped (set codacyApiToken and/or codacyProjectToken in local.properties).",
            )
            return
        }
        logger.lifecycle("Codacy: uploading ${report.name} (best-effort)…")
        val result = runCatching {
            execQuiet(rootDir) {
                environment("CODACY_API_TOKEN", apiToken.orEmpty())
                environment("CODACY_PROJECT_TOKEN", projectToken.orEmpty())
                // Official installer script; force Kotlin + Jacoco parser like CI.
                commandLine(
                    "bash",
                    "-lc",
                    """
                    set -euo pipefail
                    curl -Ls https://coverage.codacy.com/get.sh -o build/codacy-coverage.sh
                    chmod +x build/codacy-coverage.sh
                    bash build/codacy-coverage.sh report \
                      --force-language \
                      -l Kotlin \
                      --force-coverage-parser jacoco \
                      -r '${report.absolutePath}'
                    """.trimIndent(),
                )
            }
        }
        result.fold(
            onSuccess = { exec ->
                if (exec.exitValue == 0) {
                    logger.lifecycle("Codacy: upload OK.")
                } else {
                    logger.warn("Codacy: upload failed (exit ${exec.exitValue}); continuing.")
                }
            },
            onFailure = { e ->
                logger.warn("Codacy: upload error (${e.message}); continuing.")
            },
        )
    }

    private fun execQuiet(rootDir: java.io.File, configure: org.gradle.process.ExecSpec.() -> Unit): ExecResult {
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        return execOperations.exec {
            workingDir = rootDir
            isIgnoreExitValue = true
            standardOutput = stdout
            errorOutput = stderr
            configure()
        }.also {
            if (it.exitValue != 0) {
                logger.info(stdout.toString(Charsets.UTF_8))
                logger.info(stderr.toString(Charsets.UTF_8))
            }
        }
    }

    private fun loadLocalProperties(rootDir: java.io.File): Properties {
        val props = Properties()
        val file = rootDir.resolve("local.properties")
        if (file.isFile) {
            file.reader(Charsets.UTF_8).use { props.load(it) }
        }
        // Prefer process env as fallback (same names CI uses).
        listOf(
            "CODECOV_TOKEN",
            "CODACY_API_TOKEN",
            "CODACY_PROJECT_TOKEN",
        ).forEach { key ->
            System.getenv(key)?.takeIf { it.isNotBlank() }?.let { props.putIfAbsent(key, it) }
        }
        return props
    }

    private fun firstProp(props: Properties, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            props.getProperty(key)?.takeIf { it.isNotBlank() }
                ?: System.getenv(key)?.takeIf { it.isNotBlank() }
        }
}
