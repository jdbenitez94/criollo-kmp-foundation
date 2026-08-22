plugins {
    alias(libs.plugins.criollo.kmp.library)
}

kotlin {
    sourceSets {
        val nonWebMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(nonWebMain)
        jvmMain.get().dependsOn(nonWebMain)
        findByName("iosMain")?.dependsOn(nonWebMain)

        commonMain.dependencies {
            api(project(":kryptostore:serializers"))
            api(libs.androidx.datastore.core.okio)
            api(libs.androidx.datastore.preferences.core)
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
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
