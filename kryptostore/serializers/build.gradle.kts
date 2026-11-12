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
            api(project(":kryptostore:crypto"))
            api(libs.androidx.datastore.core.okio)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.org.jetbrains.kotlinx.serialization.protobuf)
        }
        jvmTest.dependencies {
            implementation(libs.com.squareup.okio.fakefilesystem)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.org.jetbrains.kotlin.test)
            }
        }
    }
}
