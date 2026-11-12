package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.android

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.createEncryptedProtoDataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.createPlainProtoDataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.Cipher
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto.StoreRegistry
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences.createEncryptedPreferencesDataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.preferences.createPlainPreferencesDataStore
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.EncryptedStoreOptions
import io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.serializers.StoreLocator
import kotlinx.serialization.KSerializer
import okio.Path.Companion.toPath
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Singleton encrypted proto [DataStore] per [fileName] (REQ-AND-01).
 *
 * Builds [StoreLocator.Platform] with an app-files path and [fileName] as the web store id.
 * Never constructs EncryptedFile / MasterKeys (REQ-AND-04).
 */
fun <T : Any> encryptedProtoDataStore(
    fileName: String,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    cipher: () -> Cipher,
    options: EncryptedStoreOptions.() -> Unit = {},
    produceMigrations: (Context) -> List<DataMigration<T>> = { emptyList() },
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
    registry: StoreRegistry? = null,
): ReadOnlyProperty<Context, DataStore<T>> = ContextDataStoreSingleton(fileName) { context ->
    val opts = EncryptedStoreOptions().apply(options)
    createEncryptedProtoDataStore(
        cipher = cipher(),
        kSerializer = kSerializer,
        defaultValue = defaultValue,
        locator = StoreLocator.platform(
            producePath = { context.kryptostoreFile(fileName) },
            name = fileName,
        ),
        options = opts,
        migrations = produceMigrations(context),
        corruptionHandler = corruptionHandler,
        registry = registry,
    )
}

/**
 * Singleton encrypted Preferences [DataStore] (REQ-AND-02).
 * File is stored as `{name}.preferences_pb`; [name] is the web/localStorage id.
 */
fun encryptedPreferencesDataStore(
    name: String,
    cipher: () -> Cipher,
    options: EncryptedStoreOptions.() -> Unit = {},
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { emptyList() },
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
    registry: StoreRegistry? = null,
): ReadOnlyProperty<Context, DataStore<Preferences>> {
    val fileName = "$name.preferences_pb"
    return ContextDataStoreSingleton(fileName) { context ->
        val opts = EncryptedStoreOptions().apply(options)
        createEncryptedPreferencesDataStore(
            cipher = cipher(),
            locator = StoreLocator.platform(
                producePath = { context.kryptostoreFile(fileName) },
                name = name,
            ),
            options = opts,
            migrations = produceMigrations(context),
            corruptionHandler = corruptionHandler,
            registry = registry,
        )
    }
}

/**
 * Singleton plain Preferences [DataStore] (REQ-AND-03).
 */
fun plainPreferencesDataStore(
    name: String,
    produceMigrations: (Context) -> List<DataMigration<Preferences>> = { emptyList() },
    corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
): ReadOnlyProperty<Context, DataStore<Preferences>> {
    val fileName = "$name.preferences_pb"
    return ContextDataStoreSingleton(fileName) { context ->
        createPlainPreferencesDataStore(
            locator = StoreLocator.platform(
                producePath = { context.kryptostoreFile(fileName) },
                name = name,
            ),
            migrations = produceMigrations(context),
            corruptionHandler = corruptionHandler,
        )
    }
}

/**
 * Singleton plain proto [DataStore] for non-sensitive typed settings.
 * [fileName] is both the on-disk file and the web store id.
 */
fun <T : Any> plainProtoDataStore(
    fileName: String,
    kSerializer: KSerializer<T>,
    defaultValue: T,
    produceMigrations: (Context) -> List<DataMigration<T>> = { emptyList() },
    corruptionHandler: ReplaceFileCorruptionHandler<T>? = null,
): ReadOnlyProperty<Context, DataStore<T>> = ContextDataStoreSingleton(fileName) { context ->
    createPlainProtoDataStore(
        kSerializer = kSerializer,
        defaultValue = defaultValue,
        locator = StoreLocator.platform(
            producePath = { context.kryptostoreFile(fileName) },
            name = fileName,
        ),
        migrations = produceMigrations(context),
        corruptionHandler = corruptionHandler,
    )
}

internal fun Context.kryptostoreFile(fileName: String) =
    applicationContext.filesDir.resolve("datastore").resolve(fileName).absolutePath.toPath()

internal class ContextDataStoreSingleton<T>(private val key: String, private val create: (Context) -> DataStore<T>) :
    ReadOnlyProperty<Context, DataStore<T>> {
    private val lock = Any()

    @Volatile
    private var instance: DataStore<T>? = null

    override fun getValue(thisRef: Context, property: KProperty<*>): DataStore<T> = instance ?: synchronized(lock) {
        instance ?: create(thisRef.applicationContext).also { instance = it }
    }

    override fun toString(): String = "ContextDataStoreSingleton($key)"
}
