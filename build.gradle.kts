import io.github.jdbenitez94.criollo.kmp.foundation.buildlogic.criolloProperty

plugins {
    alias(libs.plugins.convention.root)
    alias(libs.plugins.com.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.org.jetbrains.compose) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose) apply false
    alias(libs.plugins.org.jetbrains.kotlin.multiplatform) apply false
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.org.jetbrains.dokka) apply false
    alias(libs.plugins.org.jlleitschuh.gradle.ktlint) apply false
    alias(libs.plugins.io.gitlab.arturbosch.detekt) apply false
    alias(libs.plugins.dev.detekt) apply false
    alias(libs.plugins.org.owasp.dependencycheck)
}

dependencyCheck {
    failBuildOnCVSS = 7.0f

    // Without an NVD API key, CVE DB updates are rate-limited and can take tens of minutes.
    // Request a key: https://nvd.nist.gov/developers/request-an-api-key
    // Provide via env `NVD_API_KEY`, `-PnvdApiKey=…`, or `nvdApiKey` in local.properties.
    nvd {
        apiKey = providers.environmentVariable("NVD_API_KEY").orNull
            ?: criolloProperty("nvdApiKey")
        validForHours = 24
    }

    // KMP/JVM/Android libraries — skip ecosystem analyzers we never ship.
    analyzers {
        assemblyEnabled = false
        msbuildEnabled = false
        nuspecEnabled = false
        nugetconfEnabled = false
        bundleAuditEnabled = false
        nodePackage.enabled = false
        nodeAudit.enabled = false
        retirejs.enabled = false
    }
}
