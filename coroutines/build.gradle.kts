plugins {
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform)
    alias(libs.plugins.com.android.kotlin.multiplatform.library)
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
            implementation(libs.org.jetbrains.kotlin.test)
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
    }
}
