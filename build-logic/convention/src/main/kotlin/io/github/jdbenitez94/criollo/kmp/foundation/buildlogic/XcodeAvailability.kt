package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider

const val XCODE_AVAILABLE_PROVIDER_KEY = "xcodeAvailableProvider"

fun Project.configureXcodeAvailability() {
    val os = System.getProperty("os.name")
    val provider: Provider<Boolean> =
        if (!os.contains("Mac", ignoreCase = true) && !os.contains("Darwin", ignoreCase = true)) {
            providers.provider { false }
        } else {
            providers.exec {
                commandLine("xcrun", "xcodebuild", "-version")
                isIgnoreExitValue = true
            }.result.map { it.exitValue == 0 }
        }
    rootProject.extensions.extraProperties.set(XCODE_AVAILABLE_PROVIDER_KEY, provider)
}

fun Project.xcodeAvailableProvider(): Provider<Boolean> {
    val extra = rootProject.extensions.extraProperties
    if (!extra.has(XCODE_AVAILABLE_PROVIDER_KEY)) {
        return providers.provider { false }
    }
    @Suppress("UNCHECKED_CAST")
    return extra[XCODE_AVAILABLE_PROVIDER_KEY] as Provider<Boolean>
}

fun Project.isXcodeAvailable(): Boolean = xcodeAvailableProvider().get()
