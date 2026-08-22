plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                // Shared Android/JVM Tink APIs; androidMain still uses tink-android for Keystore.
                implementation(libs.com.google.crypto.tink)
                implementation(libs.org.jetbrains.kotlinx.coroutines.core)
            }
        }
        androidMain {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                implementation(libs.com.google.crypto.tink.android)
            }
        }
        jvmMain {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                implementation(libs.com.google.crypto.tink)
                implementation(libs.net.java.dev.jna)
                implementation(libs.net.java.dev.jna.platform)
            }
        }
        commonMain.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.test)
        }
        iosMain.dependencies {
            implementation(libs.dev.whyoleg.cryptography.core)
            implementation(libs.dev.whyoleg.cryptography.provider.apple)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
    }
}
