package io.github.jdbenitez94.criollo.kmp.foundation.kryptostore

internal const val INDEXED_DB_INIT_FUNCTION: String = """() => {
    if (globalThis.__kryptostore_idb) return;

    function base64ToBytes(base64) {
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) {
            bytes[i] = binary.charCodeAt(i);
        }
        return bytes;
    }

    function bytesToBase64(bytes) {
        let binary = '';
        for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }

    function openDb(databaseName, version, storeName, keyPath) {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(databaseName, version);
            request.onupgradeneeded = () => {
                const db = request.result;
                if (!db.objectStoreNames.contains(storeName)) {
                    db.createObjectStore(storeName, { keyPath });
                }
            };
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    function idbGet(db, storeName, key) {
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, 'readonly');
            const request = transaction.objectStore(storeName).get(key);
            request.onsuccess = () => resolve(request.result !== undefined ? request.result : null);
            request.onerror = () => reject(request.error);
        });
    }

    function idbPut(db, storeName, value) {
        return new Promise((resolve, reject) => {
            const transaction = db.transaction(storeName, 'readwrite');
            transaction.oncomplete = () => resolve(true);
            transaction.onerror = () => reject(transaction.error);
            transaction.objectStore(storeName).put(value);
        });
    }

    const PROTO_DB_NAME = 'app-proto';
    const PROTO_STORE_NAME = 'stores';
    const LEGACY_PROTO_DB_NAME = 'saveable-proto';

    let legacyProtoDbWiped = false;
    let protoDbPromise = null;

    function openProtoDb() {
        if (protoDbPromise == null) {
            protoDbPromise = openDb(PROTO_DB_NAME, 1, PROTO_STORE_NAME, 'id');
        }
        return protoDbPromise;
    }

    function protoBroadcast(name, version) {
        if (typeof BroadcastChannel === 'undefined') return;
        const channel = new BroadcastChannel('app-proto-sync');
        channel.postMessage({ name, version });
        channel.close();
    }

    function recordToBase64(record) {
        if (record == null) return null;
        if (record.bytes != null) return bytesToBase64(record.bytes);
        return record.bytesBase64 !== undefined ? record.bytesBase64 : null;
    }

    function lockResolvers() {
        let resolvers = globalThis.__appProtoLockResolvers;
        if (!resolvers) {
            resolvers = {};
            globalThis.__appProtoLockResolvers = resolvers;
        }
        return resolvers;
    }

    globalThis.__kryptostore_idb = {
        install: function() {
            if (legacyProtoDbWiped || typeof indexedDB === 'undefined') return;
            legacyProtoDbWiped = true;
            try {
                indexedDB.deleteDatabase(LEGACY_PROTO_DB_NAME);
            } catch (_error) {}
        },
        read: async function(name) {
            const db = await openProtoDb();
            const record = await idbGet(db, PROTO_STORE_NAME, name);
            return recordToBase64(record);
        },
        write: async function(name, payloadBase64) {
            const db = await openProtoDb();
            const record = await idbGet(db, PROTO_STORE_NAME, name);
            const currentVersion = (record && record.version !== undefined) ? record.version : 0;
            await idbPut(db, PROTO_STORE_NAME, {
                id: name,
                version: currentVersion,
                bytes: base64ToBytes(payloadBase64),
                updatedAt: Date.now(),
            });
            return currentVersion;
        },
        version: async function(name) {
            const db = await openProtoDb();
            const record = await idbGet(db, PROTO_STORE_NAME, name);
            return (record && record.version !== undefined) ? record.version : 0;
        },
        incrementVersion: async function(name) {
            const db = await openProtoDb();
            const record = await idbGet(db, PROTO_STORE_NAME, name);
            const currentVersion = (record && record.version !== undefined) ? record.version : 0;
            const nextVersion = currentVersion + 1;
            const bytes = (record && record.bytes !== undefined)
                ? record.bytes
                : ((record && record.bytesBase64 !== undefined) ? base64ToBytes(record.bytesBase64) : null);
            await idbPut(db, PROTO_STORE_NAME, {
                id: name,
                version: nextVersion,
                bytes: bytes,
                updatedAt: Date.now(),
            });
            protoBroadcast(name, nextVersion);
            return nextVersion;
        },
        lock: async function(name) {
            const lockName = 'app-proto:' + name;
            if (!globalThis.navigator || !globalThis.navigator.locks) return true;
            const resolvers = lockResolvers();
            await new Promise((resolve, reject) => {
                navigator.locks.request(lockName, { mode: 'exclusive' }, () => {
                    resolve();
                    return new Promise((release) => {
                        resolvers[lockName] = release;
                    });
                }).catch(reject);
            });
            return true;
        },
        tryLock: async function(name) {
            const lockName = 'app-proto:' + name;
            if (!globalThis.navigator || !globalThis.navigator.locks) return true;
            const resolvers = lockResolvers();
            let acquired = false;
            await navigator.locks.request(lockName, { mode: 'exclusive', ifAvailable: true }, (lock) => {
                if (lock == null) return Promise.resolve(false);
                acquired = true;
                return new Promise((release) => {
                    resolvers[lockName] = release;
                });
            });
            return acquired;
        },
        unlock: async function(name) {
            const lockName = 'app-proto:' + name;
            const resolvers = globalThis.__appProtoLockResolvers;
            if (!resolvers) return true;
            const resolver = resolvers[lockName];
            if (resolver) {
                resolver(true);
                delete resolvers[lockName];
            }
            return true;
        }
    };
}"""

internal const val INDEXED_DB_INIT_SCRIPT: String = "($INDEXED_DB_INIT_FUNCTION)()"
