import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.ProjectConfig
import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloResolvedVersion
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-platform`
    alias(libs.plugins.criollo.maven.publish)
}

group = ProjectConfig.group
version = criolloResolvedVersion()

dependencies {
    constraints {
        api(project(":coroutines"))
        api(project(":coroutines:compose"))
        api(project(":coroutines:viewmodel"))
        api(project(":kryptostore"))
        api(project(":kryptostore:crypto"))
        api(project(":kryptostore:serializers"))
        api(project(":kryptostore:preferences"))
        api(project(":kryptostore:android"))
        api(project(":kryptostore:migrate-android"))
        api(project(":project-conventions"))

        // Align transitive crypto / DataStore versions for kryptostore consumers (REQ-PKG-03).
        api(libs.androidx.datastore.core.okio)
        api(libs.androidx.datastore.preferences.core)
        api(libs.com.google.crypto.tink)
        api(libs.com.google.crypto.tink.android)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["javaPlatform"])
        }
    }
}
