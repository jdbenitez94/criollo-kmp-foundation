package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Initializes platform crypto (key rotation + re-encrypt + post-init hooks) behind a mutex.
 *
 * [cipher] is only safe to use after [state] is [CryptoRuntimeState.Ready].
 *
 * On rotation the order is: rotate → postRotationInit → [StoreRegistry.reEncryptAll] →
 * postMigrationCleanup → Ready (REQ-ROT-02). Re-encrypt runs before cleanup so old key
 * material can still decrypt existing ciphertext (notably iOS SecureKeyStore).
 */
class CryptoRuntime(private val stack: PlatformCryptoStack, private val storeRegistry: StoreRegistry = StoreRegistry(), private val log: KryptoLog = KryptoLog.NoOp) {
    constructor(
        keyRotator: KeyRotator,
        postRotationInit: suspend () -> Unit,
        postMigrationCleanup: suspend () -> Unit = {},
        cipher: Cipher,
        storeRegistry: StoreRegistry = StoreRegistry(),
        log: KryptoLog = KryptoLog.NoOp,
    ) : this(
        stack = PlatformCryptoStack(
            cipher = cipher,
            keyRotator = keyRotator,
            postRotationInit = postRotationInit,
            postMigrationCleanup = postMigrationCleanup,
        ),
        storeRegistry = storeRegistry,
        log = log,
    )

    val cipher: Cipher
        get() = stack.cipher

    /** Registry used for post-rotation re-encrypt; register encrypted stores here. */
    val registry: StoreRegistry
        get() = storeRegistry

    val state: StateFlow<CryptoRuntimeState>
        field = MutableStateFlow<CryptoRuntimeState>(CryptoRuntimeState.Initializing)

    private val initializeMutex = Mutex()

    suspend fun initialize() {
        initializeMutex.withLock {
            if (state.value == CryptoRuntimeState.Ready) return
            state.value = CryptoRuntimeState.Initializing
            try {
                val rotationOccurred = stack.keyRotator.rotateKeyIfNeeded()
                stack.postRotationInit()
                if (rotationOccurred) {
                    storeRegistry.reEncryptAll()
                    stack.postMigrationCleanup()
                }
                state.value = CryptoRuntimeState.Ready
            } catch (e: Exception) {
                log.error(e) { "Unable to initialize crypto runtime" }
                state.value = CryptoRuntimeState.Error(e)
            }
        }
    }
}
