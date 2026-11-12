plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(project(":kryptostore:crypto"))
            api(project(":kryptostore:serializers"))
        }
        androidMain.dependencies {
            implementation(libs.com.google.crypto.tink.android)
        }
        jvmTest.dependencies {
            implementation(libs.com.google.crypto.tink)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.com.google.crypto.tink.android)
            }
        }
    }
}
