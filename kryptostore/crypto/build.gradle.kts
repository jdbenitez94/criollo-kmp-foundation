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
        iosMain.dependencies {
            implementation(libs.dev.whyoleg.cryptography.core)
            implementation(libs.dev.whyoleg.cryptography.provider.apple)
        }
    }
}
