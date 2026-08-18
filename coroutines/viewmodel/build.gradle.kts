plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.kotlin.multiplatform.library)
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
            implementation(libs.org.jetbrains.kotlin.test)
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
    }
}
