package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

private val CANONICAL_IDS = mapOf(
    ":coroutines" to ProjectConfig.Artifacts.coroutines,
    ":coroutines:compose" to ProjectConfig.Artifacts.coroutinesCompose,
    ":coroutines:viewmodel" to ProjectConfig.Artifacts.coroutinesViewmodel,
    ":result" to ProjectConfig.Artifacts.result,
    ":runtime" to ProjectConfig.Artifacts.runtime,
    ":kryptostore" to ProjectConfig.Artifacts.kryptostore,
    ":kryptostore:crypto" to ProjectConfig.Artifacts.kryptostoreCrypto,
    ":kryptostore:serializers" to ProjectConfig.Artifacts.kryptostoreSerializers,
    ":kryptostore:preferences" to ProjectConfig.Artifacts.kryptostorePreferences,
    ":kryptostore:android" to ProjectConfig.Artifacts.kryptostoreAndroid,
    ":kryptostore:migrate-android" to ProjectConfig.Artifacts.kryptostoreMigrateAndroid,
    ":bom" to ProjectConfig.Artifacts.bom,
    ":testing" to ProjectConfig.Artifacts.testing,
    ":project-conventions" to ProjectConfig.Artifacts.projectConventions,
)

internal fun canonicalArtifactId(projectPath: String, projectName: String): String =
    CANONICAL_IDS[projectPath] ?: projectName

private val POM_DESCRIPTIONS = mapOf(
    ProjectConfig.Artifacts.bom to
        "Bill of Materials for Criollo KMP Foundation artifacts.",
    ProjectConfig.Artifacts.coroutines to
        "Keyed TaskScope coroutine registry for Kotlin Multiplatform.",
    ProjectConfig.Artifacts.coroutinesCompose to
        "Compose rememberTaskScope() adapter for Criollo TaskScope.",
    ProjectConfig.Artifacts.coroutinesViewmodel to
        "ViewModel taskScope() property delegate for Criollo TaskScope.",
    ProjectConfig.Artifacts.result to
        "Flow Result triad (Loading / Success / Error) and asResult() for Kotlin Multiplatform.",
    ProjectConfig.Artifacts.runtime to
        "Shared runtime helpers for Criollo KMP (cancellable runCatching, Result extensions, retry with backoff).",
    ProjectConfig.Artifacts.kryptostore to
        "Encrypted typed DataStore factories and IndexedDB storage for KryptoStore.",
    ProjectConfig.Artifacts.kryptostoreCrypto to
        "Platform crypto (Tink / Keystore / WebCrypto) for KryptoStore encrypted DataStore.",
    ProjectConfig.Artifacts.kryptostoreSerializers to
        "Encrypted Okio serializers and fail-closed corruption handling for KryptoStore.",
    ProjectConfig.Artifacts.kryptostorePreferences to
        "Encrypted and plain Preferences DataStore factories for KryptoStore.",
    ProjectConfig.Artifacts.kryptostoreAndroid to
        "Android Context property delegates for KryptoStore (artifact kryptostore-android-delegates).",
    ProjectConfig.Artifacts.kryptostoreMigrateAndroid to
        "Optional Android migration helpers onto KryptoStore envelopes (unenveloped AEAD, guides).",
    ProjectConfig.Artifacts.testing to
        "JUnit 5 helpers for Criollo KMP Foundation consumers (Main dispatcher test extension).",
    ProjectConfig.Artifacts.projectConventions to
        "Gradle plugin that syncs Criollo .editorconfig and Detekt configs into consumer projects.",
)

internal fun pomDescriptionFor(artifactId: String): String =
    POM_DESCRIPTIONS[artifactId] ?: "Criollo KMP Foundation library module."
