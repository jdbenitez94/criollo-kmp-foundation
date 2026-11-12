import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.io.File as ProjectRootDir

/**
 * Optional local DX: markdownlint + jscpd (Codacy-style duplication), then optional Kover XML +
 * best-effort Codecov / Codacy coverage uploads when tokens exist in `local.properties`.
 *
 * Complexity for library modules is covered by Detekt (`./gradlew detekt` / `qualityCheck`);
 * `build-logic` sources are covered via `:build-logic:convention:detekt` (same qualityCheck).
 *
 * Enable coverage path with `-PlocalCloudParity.coverage=true` (or the same key in
 * `local.properties` / `gradle.properties`).
 */
@UntrackedTask(because = "Invokes network CLIs and local tooling outside Gradle inputs.")
abstract class LocalCloudParityTask @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
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
        uploadCodecovCoverage(logger, execOperations, rootDir, report, props, codecovSlug.get())
        uploadCodacyCoverage(logger, execOperations, rootDir, report, props)
    }

    private fun runMarkdownlint(rootDir: ProjectRootDir) {
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

    private fun runJscpd(rootDir: ProjectRootDir) {
        logger.lifecycle("Running jscpd (duplication, Codacy-compatible)…")
        val config = rootDir.resolve(".jscpd.json")
        val command = mutableListOf("npx", "--yes", "jscpd", ".")
        command += if (config.isFile) {
            listOf("--config", config.absolutePath)
        } else {
            listOf(
                "--format",
                "kotlin",
                "--threshold",
                "5",
                "--ignore",
                "**/build/**,**/.gradle/**,**/node_modules/**,**/.git/**,**/.venv/**,**/site/**,**/iosApp/**",
            )
        }
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
}
