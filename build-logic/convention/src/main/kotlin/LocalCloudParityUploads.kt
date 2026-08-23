import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties

internal fun uploadCodecovCoverage(logger: Logger, execOperations: ExecOperations, rootDir: File, report: File, props: Properties, codecovSlug: String) {
    val token = firstLocalProp(props, "codecovRepositoryToken", "codecovApiToken", "CODECOV_TOKEN")
    if (token == null) {
        logger.lifecycle(
            "Codecov: skipped (set codecovRepositoryToken or codecovApiToken in local.properties).",
        )
        return
    }
    logger.lifecycle("Codecov: uploading ${report.name} (best-effort)…")
    val result = runCatching {
        execQuiet(execOperations, logger, rootDir) {
            commandLine(
                "npx",
                "--yes",
                "codecov",
                "--token=$token",
                "--file=${report.absolutePath}",
                "--slug=$codecovSlug",
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

internal fun uploadCodacyCoverage(logger: Logger, execOperations: ExecOperations, rootDir: File, report: File, props: Properties) {
    val apiToken = firstLocalProp(props, "codacyApiToken", "codacyToken", "CODACY_API_TOKEN")
    val projectToken = firstLocalProp(props, "codacyProjectToken", "CODACY_PROJECT_TOKEN")
    if (apiToken == null && projectToken == null) {
        logger.lifecycle(
            "Codacy: skipped (set codacyApiToken and/or codacyProjectToken in local.properties).",
        )
        return
    }
    logger.lifecycle("Codacy: uploading ${report.name} (best-effort)…")
    val result = runCatching {
        execQuiet(execOperations, logger, rootDir) {
            environment("CODACY_API_TOKEN", apiToken.orEmpty())
            environment("CODACY_PROJECT_TOKEN", projectToken.orEmpty())
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

internal fun loadLocalProperties(rootDir: File): Properties {
    val props = Properties()
    val file = rootDir.resolve("local.properties")
    if (file.isFile) {
        file.reader(Charsets.UTF_8).use { props.load(it) }
    }
    listOf(
        "CODECOV_TOKEN",
        "CODACY_API_TOKEN",
        "CODACY_PROJECT_TOKEN",
    ).forEach { key ->
        System.getenv(key)?.takeIf { it.isNotBlank() }?.let { props.putIfAbsent(key, it) }
    }
    return props
}

internal fun firstLocalProp(props: Properties, vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    props.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(key)?.takeIf { it.isNotBlank() }
}

private fun execQuiet(execOperations: ExecOperations, logger: Logger, rootDir: File, configure: ExecSpec.() -> Unit): ExecResult {
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
