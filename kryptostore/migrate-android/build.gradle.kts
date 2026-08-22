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
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.com.google.crypto.tink.android)
        }
        configureEach {
            if (name == "jvmTest" || name == "androidHostTest") {
                dependencies {
                    implementation(libs.org.jetbrains.kotlinx.coroutines.test)
                    implementation(project.dependencies.platform(libs.io.strikt.bom))
                    implementation(libs.io.strikt.core)
                }
            }
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
