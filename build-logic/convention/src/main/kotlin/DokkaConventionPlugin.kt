import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.libsVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class DokkaConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.dokka")
        target.forcePatchedJacksonOnDokkaClasspaths()
    }

    /**
     * Dokka 2.2.0 pins Jackson 2.15.3. Force a patched line that includes
     * CVE-2026-54512/54513 fixes. Dokka 2.1+ supports user Jackson upgrades.
     */
    private fun Project.forcePatchedJacksonOnDokkaClasspaths() {
        val jackson = libsVersion("jackson")
        configurations.configureEach {
            resolutionStrategy.eachDependency {
                if (!requested.group.startsWith("com.fasterxml.jackson")) return@eachDependency
                // jackson-annotations is versioned as 2.22 (no patch); databind is 2.22.2.
                if (requested.name == "jackson-annotations") return@eachDependency
                useVersion(jackson)
                because("Dokka 2.2.0 ships Jackson 2.15.3 (CVE-2026-54512/54513)")
            }
        }
    }
}
