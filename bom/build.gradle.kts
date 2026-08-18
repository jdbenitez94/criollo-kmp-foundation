import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-platform`
    alias(libs.plugins.criollo.maven.publish)
}

group = ProjectConfig.group
version = ProjectConfig.version

dependencies {
    constraints {
        api(project(":coroutines"))
        api(project(":coroutines:compose"))
        api(project(":coroutines:viewmodel"))
        api(project(":project-conventions"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["javaPlatform"])
        }
    }
}
