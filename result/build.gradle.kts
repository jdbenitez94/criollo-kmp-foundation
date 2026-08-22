plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
    }
}
