plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":kryptostore:serializers"))
            api(libs.androidx.datastore.core.okio)
            api(libs.androidx.datastore.preferences.core)
        }
    }
}
