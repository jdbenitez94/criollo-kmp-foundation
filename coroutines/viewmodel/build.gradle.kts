plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":coroutines"))
            implementation(libs.org.jetbrains.androidx.lifecycle.viewmodel)
            implementation(libs.org.jetbrains.androidx.lifecycle.viewmodel.compose)
        }
    }
}
