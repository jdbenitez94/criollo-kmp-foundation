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
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain {
            dependsOn(jvmAndAndroidMain)
        }
        jvmMain {
            dependsOn(jvmAndAndroidMain)
        }
        commonMain.dependencies {
            // Android-only DX; other targets publish empty metadata artifacts.
            api(project(":kryptostore"))
            api(project(":kryptostore:preferences"))
            api(project(":kryptostore:crypto"))
            api(libs.androidx.datastore.core.okio)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.org.jetbrains.kotlinx.serialization.protobuf)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.org.jetbrains.kotlinx.serialization.protobuf)
            }
        }
    }
}
