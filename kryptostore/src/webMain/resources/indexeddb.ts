import webUtils = require('./web-utils');

const PROTO_DB_NAME = 'app-proto';
const PROTO_STORE_NAME = 'stores';
const LEGACY_PROTO_DB_NAME = 'saveable-proto';

let legacyProtoDbWiped = false;
let protoDbPromise: Promise<IDBDatabase> | null = null;

interface ProtoRecord {
    id: string;
    version: number;
    /** Structured-clone binary payload (no Base64 tax in IndexedDB). */
    bytes: Uint8Array | null;
    /** Legacy field kept for one-shot reads of older app-proto rows. */
    bytesBase64?: string | null;
    updatedAt: number;
}

declare global {
    interface Window {
        __appProtoLockResolvers?: Record<string, (value: boolean) => void>;
    }

    var __appProtoLockResolvers: Record<string, (value: boolean) => void> | undefined;
}

function openProtoDb(): Promise<IDBDatabase> {
    if (protoDbPromise == null) {
        protoDbPromise = webUtils.openDb(PROTO_DB_NAME, 1, PROTO_STORE_NAME, 'id');
    }
    return protoDbPromise as Promise<IDBDatabase>;
}

async function protoGet(db: IDBDatabase, id: string): Promise<ProtoRecord | null> {
    return (await webUtils.idbGet(db, PROTO_STORE_NAME, id)) as ProtoRecord | null;
}

async function protoPut(db: IDBDatabase, value: ProtoRecord): Promise<boolean> {
    return webUtils.idbPut(db, PROTO_STORE_NAME, value);
}

function protoBroadcast(name: string, version: number): void {
    if (typeof BroadcastChannel === 'undefined') {
        return;
    }
    const channel = new BroadcastChannel('app-proto-sync');
    channel.postMessage({ name, version });
    channel.close();
}

function recordToBase64(record: ProtoRecord | null): string | null {
    if (record == null) {
        return null;
    }
    if (record.bytes != null) {
        return webUtils.bytesToBase64(record.bytes);
    }
    return record.bytesBase64 ?? null;
}

function base64ToBytes(payloadBase64: string): Uint8Array {
    return webUtils.base64ToBytes(payloadBase64);
}

async function read(name: string): Promise<string | null> {
    const db = await openProtoDb();
    const record = await protoGet(db, name);
    return recordToBase64(record);
}

async function write(name: string, payloadBase64: string): Promise<number> {
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

async function version(name: string): Promise<number> {
    const db = await openProtoDb();
    const record = await protoGet(db, name);
    return record?.version ?? 0;
}

async function incrementVersion(name: string): Promise<number> {
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

async function lock(name: string): Promise<boolean> {
    const lockName = `app-proto:${name}`;
    if (!globalThis.navigator?.locks) {
        return true;
    }

    globalThis.__appProtoLockResolvers ??= {};
    await new Promise<void>((resolve, reject) => {
        navigator.locks
            .request(lockName, { mode: 'exclusive' }, () => {
                resolve();
                return new Promise<boolean>((release) => {
                    globalThis.__appProtoLockResolvers![lockName] = release;
                });
            })
            .catch((error) => {
                reject(error);
            });
    });
    return true;
}

/**
 * Non-blocking lock attempt using Web Locks `ifAvailable`.
 * @returns true when the lock was acquired, false when another holder has it.
 */
async function tryLock(name: string): Promise<boolean> {
    const lockName = `app-proto:${name}`;
    if (!globalThis.navigator?.locks) {
        return true;
    }

    globalThis.__appProtoLockResolvers ??= {};
    let acquired = false;
    await navigator.locks.request(lockName, { mode: 'exclusive', ifAvailable: true }, (lock) => {
        if (lock == null) {
            return Promise.resolve(false);
        }
        acquired = true;
        return new Promise<boolean>((release) => {
            globalThis.__appProtoLockResolvers![lockName] = release;
        });
    });
    return acquired;
}

async function unlock(name: string): Promise<boolean> {
    const lockName = `app-proto:${name}`;
    const resolver = globalThis.__appProtoLockResolvers?.[lockName];
    if (resolver) {
        resolver(true);
        delete globalThis.__appProtoLockResolvers![lockName];
    }
    return true;
}

function install(): void {
    // Fresh baseline: the pre-rename database is encrypted with keys this build no longer holds,
    // so delete it rather than leaving unreadable records in the user's browser.
    if (legacyProtoDbWiped || typeof indexedDB === 'undefined') {
        return;
    }
    legacyProtoDbWiped = true;
    try {
        indexedDB.deleteDatabase(LEGACY_PROTO_DB_NAME);
    } catch {
        // Deleting the legacy database is best-effort; never block store initialization on it.
    }
}

const kryptoStoreIndexedDb = {
    install,
    read,
    write,
    version,
    incrementVersion,
    lock,
    tryLock,
    unlock,
};

export = kryptoStoreIndexedDb;
