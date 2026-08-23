plugins {
    alias(libs.plugins.criollo.kmp.library)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

kotlin {
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
    }
}
