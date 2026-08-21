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
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
    }
}
