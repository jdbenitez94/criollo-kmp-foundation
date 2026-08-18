plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.kotlin.multiplatform.library)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":coroutines"))
            implementation(libs.org.jetbrains.compose.runtime)
        }
        jvmTest.dependencies {
            implementation(libs.org.jetbrains.kotlin.test)
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
            implementation(libs.org.jetbrains.compose.ui.test.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
