plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.com.google.crypto.tink.android)
        }
        jvmMain.dependencies {
            implementation(libs.com.google.crypto.tink)
            implementation(libs.net.java.dev.jna)
            implementation(libs.net.java.dev.jna.platform)
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
