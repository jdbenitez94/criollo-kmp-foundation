package io.github.jdbenitez94.criollo.kmp.foundation.buildlogic

internal fun canonicalArtifactId(projectPath: String, projectName: String): String = when (projectPath) {
    ":coroutines" -> ProjectConfig.Artifacts.coroutines
    ":coroutines:compose" -> ProjectConfig.Artifacts.coroutinesCompose
    ":coroutines:viewmodel" -> ProjectConfig.Artifacts.coroutinesViewmodel
    ":result" -> ProjectConfig.Artifacts.result
    ":runtime" -> ProjectConfig.Artifacts.runtime
    ":kryptostore" -> ProjectConfig.Artifacts.kryptostore
    ":kryptostore:crypto" -> ProjectConfig.Artifacts.kryptostoreCrypto
    ":kryptostore:serializers" -> ProjectConfig.Artifacts.kryptostoreSerializers
    ":kryptostore:preferences" -> ProjectConfig.Artifacts.kryptostorePreferences
    ":kryptostore:android" -> ProjectConfig.Artifacts.kryptostoreAndroid
    ":kryptostore:migrate-android" -> ProjectConfig.Artifacts.kryptostoreMigrateAndroid
    ":bom" -> ProjectConfig.Artifacts.bom
    ":testing" -> ProjectConfig.Artifacts.testing
    ":project-conventions" -> ProjectConfig.Artifacts.projectConventions
    else -> projectName
}

internal fun pomDescriptionFor(artifactId: String): String = when (artifactId) {
    ProjectConfig.Artifacts.bom ->
        "Bill of Materials for Criollo KMP Foundation artifacts."

    ProjectConfig.Artifacts.coroutines ->
        "Keyed TaskScope coroutine registry for Kotlin Multiplatform."

    ProjectConfig.Artifacts.coroutinesCompose ->
        "Compose rememberTaskScope() adapter for Criollo TaskScope."

    ProjectConfig.Artifacts.coroutinesViewmodel ->
        "ViewModel taskScope() property delegate for Criollo TaskScope."

    ProjectConfig.Artifacts.result ->
        "Flow Result triad (Loading / Success / Error) and asResult() for Kotlin Multiplatform."

    ProjectConfig.Artifacts.runtime ->
        "Shared runtime helpers for Criollo KMP (cancellable runCatching, Result extensions, retry with backoff)."

    ProjectConfig.Artifacts.kryptostore ->
        "Encrypted typed DataStore factories and IndexedDB storage for KryptoStore."

    ProjectConfig.Artifacts.kryptostoreCrypto ->
        "Platform crypto (Tink / Keystore / WebCrypto) for KryptoStore encrypted DataStore."

    ProjectConfig.Artifacts.kryptostoreSerializers ->
        "Encrypted Okio serializers and fail-closed corruption handling for KryptoStore."

    ProjectConfig.Artifacts.kryptostorePreferences ->
        "Encrypted and plain Preferences DataStore factories for KryptoStore."

    ProjectConfig.Artifacts.kryptostoreAndroid ->
        "Android Context property delegates for KryptoStore (artifact kryptostore-android-delegates)."

    ProjectConfig.Artifacts.kryptostoreMigrateAndroid ->
        "Optional Android migration helpers onto KryptoStore envelopes (unenveloped AEAD, guides)."

    ProjectConfig.Artifacts.testing ->
        "JUnit 5 helpers for Criollo KMP Foundation consumers (Main dispatcher test extension)."

    ProjectConfig.Artifacts.projectConventions ->
        "Gradle plugin that syncs Criollo .editorconfig and Detekt configs into consumer projects."

    else -> "Criollo KMP Foundation library module."
}
