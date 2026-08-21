plugins {
    alias(libs.plugins.criollo.kmp.library)
    alias(libs.plugins.org.jetbrains.compose)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":coroutines"))
            implementation(libs.org.jetbrains.compose.runtime)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
            implementation(libs.org.jetbrains.compose.ui.test.junit4)
            implementation(compose.desktop.currentOs)
        }
    }
}
