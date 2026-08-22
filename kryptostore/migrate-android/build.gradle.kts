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
        jvmTest.dependencies {
            implementation(libs.com.google.crypto.tink)
            implementation(libs.org.jetbrains.kotlinx.coroutines.test)
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.com.google.crypto.tink.android)
                implementation(libs.org.jetbrains.kotlinx.coroutines.test)
                implementation(project.dependencies.platform(libs.io.strikt.bom))
                implementation(libs.io.strikt.core)
            }
        }
    }
}
