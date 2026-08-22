# KryptoStore migration

Guides for moving existing DataStore / encrypted-preference setups onto KryptoStore.

## Plaintext DataStore → encrypted (REQ-MIG-01)

1. Keep the same file path (or IndexedDB / localStorage name).
2. Create the encrypted store with `EncryptedStoreOptions { allowPlaintextRead = true }`.
3. On first successful read of plaintext, write any update (or `updateData { it }`) so the file is
   rewritten under the `SVBLENC1` envelope.
4. After all clients are upgraded, set `allowPlaintextRead = false` (library default).

See `PlaintextMigrationOptionTest` in `:kryptostore:serializers`.

## Unenveloped Tink AEAD blobs (REQ-MIG-02)

Older pipelines (e.g. datastore-tink `AeadSerializer`-shaped payloads) may store raw AEAD
ciphertext **without** the KryptoStore magic header.

Use `:kryptostore:migrate-android` / `LegacyAeadMigration.decryptUnenveloped(cipher, bytes, aad)`,
then re-save through `EncryptedProtoSerializer` / `EncryptedPreferencesSerializer`.

Artifact: `io.github.jdbenitez94.criollo.kmp.foundation:kryptostore-migrate-android`

## osipxd / security-crypto EncryptedFile (REQ-MIG-03)

KryptoStore **does not** depend on `androidx.security:security-crypto` or osipxd libraries.

Recommended path:

1. On a device that can still decrypt with your old stack, export plaintext values once.
2. Write them into a KryptoStore encrypted DataStore (`allowPlaintextRead` only if you stage via
   an intermediate plaintext file).
3. Remove EncryptedFile / MasterKeys usage.

If automatic decrypt of EncryptedFile is required in-process, that work must live in the **app**,
not in kryptostore (forbidden dependency).

## EncryptedSharedPreferences → Preferences DataStore (REQ-MIG-04)

1. Read keys from ESP while the old dependency is still on the app classpath.
2. Write into `createPlainPreferencesDataStore` or `createEncryptedPreferencesDataStore`
   (paths must end with `.preferences_pb`).
3. Drop ESP once migrated.

No security-crypto types are referenced from kryptostore artifacts.

## ABI dumps (REQ-HRD-02)

Kryptostore modules use `org.jetbrains.kotlinx.binary-compatibility-validator`.

```bash
./gradlew dumpKryptostoreAbi   # refresh api/**/*.api
./gradlew checkKryptostoreAbi  # compare (do not run dump+check in one Gradle invocation)
```

Note: `:kryptostore:android` Context delegates live in `androidMain`; the JVM `.api` dump for that module may be empty. Delegate behavior is covered by `androidHostTest`.

Gradle project `:kryptostore:android` publishes as **`kryptostore-android-delegates`** so it does not
collide with the KMP Android *target* publication of `:kryptostore` (`kryptostore-android`).

Fail-closed is the default: do not use replace-with-default corruption handlers for encrypted
stores (security footgun). Prefer quarantining the bad file and surfacing `CryptoRuntimeState.Error`.
