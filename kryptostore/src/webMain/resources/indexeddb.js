const webUtils = require('./web-utils');

const PROTO_DB_NAME = 'app-proto';
const PROTO_STORE_NAME = 'stores';
const LEGACY_PROTO_DB_NAME = 'saveable-proto';

let legacyProtoDbWiped = false;
let protoDbPromise = null;

function openProtoDb() {
    if (protoDbPromise == null) {
        protoDbPromise = webUtils.openDb(PROTO_DB_NAME, 1, PROTO_STORE_NAME, 'id');
    }
    return protoDbPromise;
}

async function protoGet(db, id) {
    return await webUtils.idbGet(db, PROTO_STORE_NAME, id);
}

async function protoPut(db, value) {
    return webUtils.idbPut(db, PROTO_STORE_NAME, value);
}

function protoBroadcast(name, version) {
    if (typeof BroadcastChannel === 'undefined') {
        return;
    }
    const channel = new BroadcastChannel('app-proto-sync');
    channel.postMessage({ name, version });
    channel.close();
}

function recordToBase64(record) {
    if (record == null) {
        return null;
    }
    if (record.bytes != null) {
        return webUtils.bytesToBase64(record.bytes);
    }
    return record.bytesBase64 ?? null;
}

function base64ToBytes(payloadBase64) {
    return webUtils.base64ToBytes(payloadBase64);
}

async function read(name) {
    const db = await openProtoDb();
    const record = await protoGet(db, name);
    return recordToBase64(record);
}

async function write(name, payloadBase64) {
    const db = await openProtoDb();
    const record = await protoGet(db, name);
    const currentVersion = record?.version ?? 0;
    await protoPut(db, {
        id: name,
        version: currentVersion,
        bytes: base64ToBytes(payloadBase64),
        updatedAt: Date.now(),
    });
    return currentVersion;
}

async function version(name) {
    const db = await openProtoDb();
    const record = await protoGet(db, name);
    return record?.version ?? 0;
}

async function incrementVersion(name) {
    const db = await openProtoDb();
    const record = await protoGet(db, name);
    const nextVersion = (record?.version ?? 0) + 1;
    await protoPut(db, {
        id: name,
        version: nextVersion,
        bytes: record?.bytes ?? (record?.bytesBase64 != null ? base64ToBytes(record.bytesBase64) : null),
        updatedAt: Date.now(),
    });
    protoBroadcast(name, nextVersion);
    return nextVersion;
}

function lockResolvers() {
    let resolvers = globalThis.__appProtoLockResolvers;
    if (!resolvers) {
        resolvers = {};
        globalThis.__appProtoLockResolvers = resolvers;
    }
    return resolvers;
}

async function lock(name) {
    const lockName = `app-proto:${name}`;
    if (!globalThis.navigator?.locks) {
        return true;
    }

    const resolvers = lockResolvers();
    await new Promise((resolve, reject) => {
        navigator.locks
            .request(lockName, { mode: 'exclusive' }, () => {
                resolve();
                return new Promise((release) => {
                    resolvers[lockName] = release;
                });
            })
            .catch((error) => {
                reject(error);
            });
    });
    return true;
}

async function tryLock(name) {
    const lockName = `app-proto:${name}`;
    if (!globalThis.navigator?.locks) {
        return true;
    }

    const resolvers = lockResolvers();
    let acquired = false;
    await navigator.locks.request(lockName, { mode: 'exclusive', ifAvailable: true }, (lock) => {
        if (lock == null) {
            return Promise.resolve(false);
        }
        acquired = true;
        return new Promise((release) => {
            resolvers[lockName] = release;
        });
    });
    return acquired;
}

async function unlock(name) {
    const lockName = `app-proto:${name}`;
    const resolvers = globalThis.__appProtoLockResolvers;
    if (!resolvers) {
        return true;
    }
    const resolver = resolvers[lockName];
    if (resolver) {
        resolver(true);
        delete resolvers[lockName];
    }
    return true;
}

function install() {
    if (legacyProtoDbWiped || typeof indexedDB === 'undefined') {
        return;
    }
    legacyProtoDbWiped = true;
    try {
        indexedDB.deleteDatabase(LEGACY_PROTO_DB_NAME);
    } catch (_error) {
        // Best-effort legacy wipe.
    }
}

module.exports = {
    install,
    read,
    write,
    version,
    incrementVersion,
    lock,
    tryLock,
    unlock,
};
