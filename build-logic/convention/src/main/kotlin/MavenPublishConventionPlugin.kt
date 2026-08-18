import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.configureCriolloPublishing
import org.gradle.api.Plugin
import org.gradle.api.Project

class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.configureCriolloPublishing()
    }
}
