import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@UntrackedTask(because = "Mutates local git hook configuration.")
abstract class InstallGitHooksTask @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:Internal
    abstract val rootDirProperty: DirectoryProperty

    @TaskAction
    fun run() {
        val rootDir = rootDirProperty.get().asFile
        val gitDir = File(rootDir, ".git")
        if (!gitDir.exists()) {
            logger.lifecycle("Skipping git hooks install (no .git directory).")
            return
        }

        execOperations.exec {
            commandLine("git", "config", "core.hooksPath", "gradle/hooks")
        }
        // Local-only alias for this clone (not --global): `git pr` → ./bin/pr
        execOperations.exec {
            commandLine("git", "config", "alias.pr", "!./bin/pr")
        }
        val hooksDir = File(rootDir, "gradle/hooks")
        if (hooksDir.exists()) {
            hooksDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.setExecutable(true)
                }
            }
        }
        File(rootDir, "bin/pr").takeIf { it.isFile }?.setExecutable(true)
        logger.lifecycle(
            "Git hooks installed (core.hooksPath=gradle/hooks). Local alias: git pr → ./bin/pr",
        )
    }
}
