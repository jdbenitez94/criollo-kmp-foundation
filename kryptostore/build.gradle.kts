plugins {
    alias(libs.plugins.criollo.kmp.library)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
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
            api(project(":kryptostore:crypto"))
            api(project(":kryptostore:serializers"))
            api(libs.androidx.datastore.core.okio)
            implementation(libs.org.jetbrains.kotlinx.serialization.protobuf)
            implementation(libs.org.jetbrains.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.org.jetbrains.kotlinx.coroutines.test)
        }
        jvmTest.dependencies {
            implementation(libs.com.squareup.okio.fakefilesystem)
            implementation(project.dependencies.platform(libs.io.strikt.bom))
            implementation(libs.io.strikt.core)
        }
    }
}
