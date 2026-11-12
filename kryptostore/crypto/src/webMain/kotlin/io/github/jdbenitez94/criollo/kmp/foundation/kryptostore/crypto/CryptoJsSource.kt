package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore.crypto

internal const val CRYPTO_INIT_FUNCTION: String = """() => {
    if (globalThis.__kryptostore_crypto) return;

    const CRYPTO_WORKER_SOURCE_LINES = [
        "const CRYPTO_DB_NAME = 'app-crypto';",
        "const CRYPTO_DB_VERSION = 1;",
        "const CRYPTO_KEYS_STORE = 'keys';",
        "const LEGACY_CRYPTO_DB_NAME = 'saveable-crypto';",
        "const MAX_KEY_ID_LENGTH = 64;",
        "const keyPromises = Object.create(null);",
        "const keyringPromises = Object.create(null);",
        "let cryptoDbPromise = null;",
        "",
        "try { indexedDB.deleteDatabase(LEGACY_CRYPTO_DB_NAME); } catch (_error) {}",
        "",
        "function base64ToBytes(base64) {",
        "    const binary = atob(base64);",
        "    const bytes = new Uint8Array(binary.length);",
        "    for (let index = 0; index < binary.length; index += 1) {",
        "        bytes[index] = binary.charCodeAt(index);",
        "    }",
        "    return bytes;",
        "}",
        "",
        "function bytesToBase64(bytes) {",
        "    let binary = '';",
        "    for (let index = 0; index < bytes.length; index += 1) {",
        "        binary += String.fromCharCode(bytes[index]);",
        "    }",
        "    return btoa(binary);",
        "}",
        "",
        "function openDb(databaseName, version) {",
        "    return new Promise((resolve, reject) => {",
        "        const request = indexedDB.open(databaseName, version);",
        "        request.onupgradeneeded = () => {",
        "            const db = request.result;",
        "            if (!db.objectStoreNames.contains(CRYPTO_KEYS_STORE)) {",
        "                db.createObjectStore(CRYPTO_KEYS_STORE, { keyPath: 'id' });",
        "            }",
        "        };",
        "        request.onsuccess = () => resolve(request.result);",
        "        request.onerror = () => reject(request.error);",
        "    });",
        "}",
        "",
        "function idbGet(db, storeName, key) {",
        "    return new Promise((resolve, reject) => {",
        "        const transaction = db.transaction(storeName, 'readonly');",
        "        const request = transaction.objectStore(storeName).get(key);",
        "        request.onsuccess = () => resolve(request.result ?? null);",
        "        request.onerror = () => reject(request.error);",
        "    });",
        "}",
        "",
        "function idbPut(db, storeName, value) {",
        "    return new Promise((resolve, reject) => {",
        "        const transaction = db.transaction(storeName, 'readwrite');",
        "        transaction.oncomplete = () => resolve(true);",
        "        transaction.onerror = () => reject(transaction.error);",
        "        transaction.objectStore(storeName).put(value);",
        "    });",
        "}",
        "",
        "function idbDelete(db, storeName, key) {",
        "    return new Promise((resolve, reject) => {",
        "        const transaction = db.transaction(storeName, 'readwrite');",
        "        transaction.oncomplete = () => resolve(true);",
        "        transaction.onerror = () => reject(transaction.error);",
        "        transaction.objectStore(storeName).delete(key);",
        "    });",
        "}",
        "",
        "function openCryptoDb() {",
        "    if (cryptoDbPromise == null) {",
        "        cryptoDbPromise = openDb(CRYPTO_DB_NAME, CRYPTO_DB_VERSION);",
        "    }",
        "    return cryptoDbPromise;",
        "}",
        "",
        "function storageKeyId(appId, keyId) {",
        "    return appId + '/' + keyId;",
        "}",
        "",
        "function metaStorageId(appId) {",
        "    return 'meta/' + appId;",
        "}",
        "",
        "function createKeyId() {",
        "    if (typeof crypto.randomUUID === 'function') {",
        "        return crypto.randomUUID();",
        "    }",
        "    const bytes = crypto.getRandomValues(new Uint8Array(16));",
        "    let out = '';",
        "    for (let i = 0; i < bytes.length; i += 1) {",
        "        out += bytes[i].toString(16).padStart(2, '0');",
        "    }",
        "    return out;",
        "}",
        "",
        "function encodeKeyIdHeader(keyId) {",
        "    const keyIdBytes = new TextEncoder().encode(keyId);",
        "    if (keyIdBytes.length === 0 || keyIdBytes.length > MAX_KEY_ID_LENGTH) {",
        "        throw new Error('Key id length out of range.');",
        "    }",
        "    const header = new Uint8Array(1 + keyIdBytes.length);",
        "    header[0] = keyIdBytes.length;",
        "    header.set(keyIdBytes, 1);",
        "    return header;",
        "}",
        "",
        "function decodeKeyIdHeader(message) {",
        "    if (message.length === 0) {",
        "        throw new Error('Ciphertext missing key-id header.');",
        "    }",
        "    const keyIdLength = message[0];",
        "    if (keyIdLength === 0 || keyIdLength > MAX_KEY_ID_LENGTH || message.length < 1 + keyIdLength) {",
        "        throw new Error('Invalid key-id header.');",
        "    }",
        "    const keyId = new TextDecoder().decode(message.subarray(1, 1 + keyIdLength));",
        "    const payload = message.subarray(1 + keyIdLength);",
        "    return { keyId: keyId, payload: payload };",
        "}",
        "",
        "async function generateAndStoreKey(db, appId, keyId) {",
        "    const key = await crypto.subtle.generateKey(",
        "        { name: 'AES-GCM', length: 256 },",
        "        false,",
        "        ['encrypt', 'decrypt'],",
        "    );",
        "    const id = storageKeyId(appId, keyId);",
        "    await idbPut(db, CRYPTO_KEYS_STORE, { id: id, key: key, createdAt: Date.now() });",
        "    keyPromises[id] = Promise.resolve(key);",
        "    return key;",
        "}",
        "",
        "async function loadKey(appId, keyId) {",
        "    const id = storageKeyId(appId, keyId);",
        "    if (keyPromises[id] != null) {",
        "        return keyPromises[id];",
        "    }",
        "    keyPromises[id] = (async () => {",
        "        const db = await openCryptoDb();",
        "        const existing = await idbGet(db, CRYPTO_KEYS_STORE, id);",
        "        if (!existing || !existing.key) {",
        "            throw new Error('Unknown crypto key id: ' + keyId);",
        "        }",
        "        return existing.key;",
        "    })();",
        "    try {",
        "        return await keyPromises[id];",
        "    } catch (error) {",
        "        delete keyPromises[id];",
        "        throw error;",
        "    }",
        "}",
        "",
        "async function loadMeta(db, appId) {",
        "    return await idbGet(db, CRYPTO_KEYS_STORE, metaStorageId(appId));",
        "}",
        "",
        "async function saveMeta(db, meta) {",
        "    await idbPut(db, CRYPTO_KEYS_STORE, meta);",
        "}",
        "",
        "async function ensureKeyring(appId) {",
        "    if (!appId) {",
        "        throw new Error('WebCrypto appId cannot be blank.');",
        "    }",
        "    if (keyringPromises[appId] != null) {",
        "        return keyringPromises[appId];",
        "    }",
        "    keyringPromises[appId] = (async () => {",
        "        const db = await openCryptoDb();",
        "        let meta = await loadMeta(db, appId);",
        "        if (meta && meta.activeKeyId && Array.isArray(meta.keyIds) && meta.keyIds.length > 0) {",
        "            await loadKey(appId, meta.activeKeyId);",
        "            return true;",
        "        }",
        "        const keyId = createKeyId();",
        "        await generateAndStoreKey(db, appId, keyId);",
        "        meta = {",
        "            id: metaStorageId(appId),",
        "            activeKeyId: keyId,",
        "            lastRotationMillis: 0,",
        "            keyIds: [keyId],",
        "        };",
        "        await saveMeta(db, meta);",
        "        return true;",
        "    })();",
        "    try {",
        "        return await keyringPromises[appId];",
        "    } finally {",
        "        delete keyringPromises[appId];",
        "    }",
        "}",
        "",
        "async function getActiveKeyId(appId) {",
        "    await ensureKeyring(appId);",
        "    const db = await openCryptoDb();",
        "    const meta = await loadMeta(db, appId);",
        "    return meta.activeKeyId;",
        "}",
        "",
        "async function listKeyIds(appId) {",
        "    await ensureKeyring(appId);",
        "    const db = await openCryptoDb();",
        "    const meta = await loadMeta(db, appId);",
        "    return JSON.stringify(meta.keyIds.slice());",
        "}",
        "",
        "async function deleteKey(appId, keyId) {",
        "    if (!keyId) {",
        "        throw new Error('keyId cannot be blank.');",
        "    }",
        "    const db = await openCryptoDb();",
        "    const meta = await loadMeta(db, appId);",
        "    if (!meta) {",
        "        return true;",
        "    }",
        "    if (meta.activeKeyId === keyId) {",
        "        throw new Error('Cannot delete the active crypto key.');",
        "    }",
        "    const id = storageKeyId(appId, keyId);",
        "    await idbDelete(db, CRYPTO_KEYS_STORE, id);",
        "    delete keyPromises[id];",
        "    meta.keyIds = meta.keyIds.filter((entry) => entry !== keyId);",
        "    await saveMeta(db, meta);",
        "    return true;",
        "}",
        "",
        "async function rotateIfNeeded(appId, periodMs, nowMillis) {",
        "    await ensureKeyring(appId);",
        "    const db = await openCryptoDb();",
        "    const meta = await loadMeta(db, appId);",
        "    const now = typeof nowMillis === 'number' ? nowMillis : Date.now();",
        "    const period = typeof periodMs === 'number' ? periodMs : 0;",
        "    if (!meta.lastRotationMillis) {",
        "        meta.lastRotationMillis = now;",
        "        await saveMeta(db, meta);",
        "        return false;",
        "    }",
        "    if ((now - meta.lastRotationMillis) <= period) {",
        "        return false;",
        "    }",
        "    const createdKeyId = createKeyId();",
        "    await generateAndStoreKey(db, appId, createdKeyId);",
        "    meta.keyIds.push(createdKeyId);",
        "    meta.activeKeyId = createdKeyId;",
        "    meta.lastRotationMillis = now;",
        "    await saveMeta(db, meta);",
        "    return true;",
        "}",
        "",
        "async function encryptAes(key, plaintextBytes, associatedDataBase64) {",
        "    const iv = crypto.getRandomValues(new Uint8Array(12));",
        "    const algorithm = { name: 'AES-GCM', iv: iv, tagLength: 128 };",
        "    if (associatedDataBase64 != null) {",
        "        algorithm.additionalData = base64ToBytes(associatedDataBase64);",
        "    }",
        "    const encryptedBuffer = await crypto.subtle.encrypt(algorithm, key, plaintextBytes);",
        "    const encrypted = new Uint8Array(encryptedBuffer);",
        "    const output = new Uint8Array(iv.length + encrypted.length);",
        "    output.set(iv, 0);",
        "    output.set(encrypted, iv.length);",
        "    return output;",
        "}",
        "",
        "async function decryptAes(key, payload, associatedDataBase64) {",
        "    if (payload.length <= 12) {",
        "        throw new Error('Ciphertext too short.');",
        "    }",
        "    const iv = payload.subarray(0, 12);",
        "    const body = payload.subarray(12);",
        "    const algorithm = { name: 'AES-GCM', iv: iv, tagLength: 128 };",
        "    if (associatedDataBase64 != null) {",
        "        algorithm.additionalData = base64ToBytes(associatedDataBase64);",
        "    }",
        "    const plainBuffer = await crypto.subtle.decrypt(algorithm, key, body);",
        "    return new Uint8Array(plainBuffer);",
        "}",
        "",
        "async function encrypt(appId, plaintextBase64, associatedDataBase64) {",
        "    await ensureKeyring(appId);",
        "    const db = await openCryptoDb();",
        "    const meta = await loadMeta(db, appId);",
        "    const key = await loadKey(appId, meta.activeKeyId);",
        "    const aesPayload = await encryptAes(key, base64ToBytes(plaintextBase64), associatedDataBase64);",
        "    const header = encodeKeyIdHeader(meta.activeKeyId);",
        "    const output = new Uint8Array(header.length + aesPayload.length);",
        "    output.set(header, 0);",
        "    output.set(aesPayload, header.length);",
        "    return bytesToBase64(output);",
        "}",
        "",
        "async function decrypt(appId, ciphertextBase64, associatedDataBase64) {",
        "    await ensureKeyring(appId);",
        "    const input = base64ToBytes(ciphertextBase64);",
        "    const decoded = decodeKeyIdHeader(input);",
        "    const key = await loadKey(appId, decoded.keyId);",
        "    const plain = await decryptAes(key, decoded.payload, associatedDataBase64);",
        "    return bytesToBase64(plain);",
        "}",
        "",
        "self.onmessage = async (event) => {",
        "    const data = event.data;",
        "    const requestId = data.requestId;",
        "    const type = data.type;",
        "    const appId = data.appId;",
        "    try {",
        "        let result;",
        "        if (type === 'ensureKeyring') {",
        "            result = await ensureKeyring(appId);",
        "        } else if (type === 'encrypt') {",
        "            result = await encrypt(appId, data.plaintextBase64, (data.associatedDataBase64 || null));",
        "        } else if (type === 'decrypt') {",
        "            result = await decrypt(appId, data.ciphertextBase64, (data.associatedDataBase64 || null));",
        "        } else if (type === 'rotateIfNeeded') {",
        "            result = await rotateIfNeeded(appId, data.periodMs, data.nowMillis);",
        "        } else if (type === 'listKeyIds') {",
        "            result = await listKeyIds(appId);",
        "        } else if (type === 'deleteKey') {",
        "            result = await deleteKey(appId, data.keyId);",
        "        } else if (type === 'getActiveKeyId') {",
        "            result = await getActiveKeyId(appId);",
        "        } else {",
        "            throw new Error('Unknown crypto worker request type: ' + type);",
        "        }",
        "        self.postMessage({ requestId: requestId, ok: true, result: result });",
        "    } catch (error) {",
        "        const message = error instanceof Error ? error.message : String(error);",
        "        self.postMessage({ requestId: requestId, ok: false, error: message });",
        "    }",
        "};",
    ];

    const cryptoWorkerScript = CRYPTO_WORKER_SOURCE_LINES.join('\n');
    const WORKER_REQUEST_TIMEOUT_MS = 15000;

    let cryptoWorker = null;
    let cryptoWorkerUrl = null;
    let nextRequestId = 0;
    const pendingRequests = new Map();

    function rejectAllPending(reason) {
        pendingRequests.forEach(({ reject, timer }) => {
            clearTimeout(timer);
            reject(reason);
        });
        pendingRequests.clear();
    }

    function terminateCryptoWorker() {
        if (cryptoWorker != null) {
            cryptoWorker.terminate();
            cryptoWorker = null;
        }
        if (cryptoWorkerUrl != null) {
            URL.revokeObjectURL(cryptoWorkerUrl);
            cryptoWorkerUrl = null;
        }
    }

    function ensureCryptoWorker() {
        if (cryptoWorker != null) {
            return cryptoWorker;
        }
        const blob = new Blob([cryptoWorkerScript], { type: 'application/javascript' });
        cryptoWorkerUrl = URL.createObjectURL(blob);
        cryptoWorker = new Worker(cryptoWorkerUrl);
        cryptoWorker.onmessage = (event) => {
            const { requestId, ok, result, error } = event.data;
            const pending = pendingRequests.get(requestId);
            if (!pending) {
                return;
            }
            clearTimeout(pending.timer);
            pendingRequests.delete(requestId);
            if (ok) {
                pending.resolve(result);
            } else {
                pending.reject(new Error(error || 'Crypto worker request failed.'));
            }
        };
        cryptoWorker.onerror = (event) => {
            rejectAllPending(new Error(event.message || 'Crypto worker crashed.'));
            terminateCryptoWorker();
        };
        return cryptoWorker;
    }

    function postToWorker(request) {
        const worker = ensureCryptoWorker();
        const requestId = 'crypto-' + (nextRequestId++);
        return new Promise((resolve, reject) => {
            const timer = setTimeout(() => {
                pendingRequests.delete(requestId);
                reject(new Error('Crypto worker request timed out after ' + WORKER_REQUEST_TIMEOUT_MS + 'ms.'));
            }, WORKER_REQUEST_TIMEOUT_MS);
            pendingRequests.set(requestId, { resolve, reject, timer });
            worker.postMessage(Object.assign({}, request, { requestId: requestId }));
        });
    }

    globalThis.__kryptostore_crypto = {
        install: function() {
            const subtle = globalThis.crypto ? globalThis.crypto.subtle : null;
            if (!subtle) {
                throw new Error(
                    'WebCrypto (crypto.subtle) is unavailable. ' +
                    'KryptoStore requires a secure context (HTTPS or localhost).'
                );
            }
        },
        ensureKeyring: function(appId) {
            return postToWorker({ type: 'ensureKeyring', appId: appId }).then(Boolean);
        },
        encrypt: function(appId, plaintextBase64, associatedDataBase64) {
            return postToWorker({
                type: 'encrypt',
                appId: appId,
                plaintextBase64: plaintextBase64,
                associatedDataBase64: associatedDataBase64,
            }).then(String);
        },
        decrypt: function(appId, ciphertextBase64, associatedDataBase64) {
            return postToWorker({
                type: 'decrypt',
                appId: appId,
                ciphertextBase64: ciphertextBase64,
                associatedDataBase64: associatedDataBase64,
            }).then(String);
        },
        rotateIfNeeded: function(appId, periodMs, nowMillis) {
            return postToWorker({
                type: 'rotateIfNeeded',
                appId: appId,
                periodMs: periodMs,
                nowMillis: nowMillis,
            }).then(Boolean);
        },
        listKeyIds: function(appId) {
            return postToWorker({ type: 'listKeyIds', appId: appId }).then(String);
        },
        deleteKey: function(appId, keyId) {
            return postToWorker({ type: 'deleteKey', appId: appId, keyId: keyId }).then(Boolean);
        },
        getActiveKeyId: function(appId) {
            return postToWorker({ type: 'getActiveKeyId', appId: appId }).then(String);
        },
        dispose: terminateCryptoWorker,
    };
}"""

internal const val CRYPTO_INIT_SCRIPT: String = "($CRYPTO_INIT_FUNCTION)()"
