plugins {
    alias(libs.plugins.criollo.kmp.library)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            // Android-only DX; other targets publish empty metadata artifacts.
            api(project(":kryptostore"))
            api(project(":kryptostore:preferences"))
            api(project(":kryptostore:crypto"))
            api(libs.androidx.datastore.core.okio)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.org.jetbrains.kotlinx.serialization.protobuf)
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.coroutines.test)
                implementation(libs.org.jetbrains.kotlinx.serialization.protobuf)
                implementation(project.dependencies.platform(libs.io.strikt.bom))
                implementation(libs.io.strikt.core)
            }
        }
    }
}
