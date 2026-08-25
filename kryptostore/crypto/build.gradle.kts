import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension

plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        withHostTest {}
    }
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

extensions.configure<KoverProjectExtension> {
    reports {
        filters {
            excludes {
                // AndroidKeyStore is unavailable in JVM host tests; all surrounding Android orchestration
                // is covered through an injected in-memory master AEAD.
                classes(
                    "io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto." +
                        "AndroidKeystoreMasterAeadFactory",
                    // Kotlin emits this uncallable JVM bridge; '?' matches the '$' that Kover
                    // otherwise treats as an end-anchor in class-name filters.
                    "io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto." +
                        "AlgorithmProvider?DefaultImpls",
                )
            }
        }
    }
}
