type IdbUpgradeHandler = (db: IDBDatabase) => void;

function base64ToBytes(base64: string): Uint8Array {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.charCodeAt(index);
    }
    return bytes;
}

function bytesToBase64(bytes: Uint8Array): string {
    let binary = '';
    for (let index = 0; index < bytes.length; index += 1) {
        binary += String.fromCharCode(bytes[index]);
    }
    return btoa(binary);
}

function openDb(
    databaseName: string,
    version: number,
    storeName: string,
    keyPath: string,
    onUpgrade?: IdbUpgradeHandler,
): Promise<IDBDatabase> {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(databaseName, version);
        request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains(storeName)) {
                db.createObjectStore(storeName, { keyPath });
            }
            onUpgrade?.(db);
        };
        request.onsuccess = () => {
            resolve(request.result);
        };
        request.onerror = () => {
            reject(request.error);
        };
    });
}

function idbGet(db: IDBDatabase, storeName: string, key: string): Promise<unknown | null> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(storeName, 'readonly');
        const request = transaction.objectStore(storeName).get(key);
        request.onsuccess = () => {
            resolve(request.result ?? null);
        };
        request.onerror = () => {
            reject(request.error);
        };
    });
}

function idbPut(db: IDBDatabase, storeName: string, value: unknown): Promise<boolean> {
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(storeName, 'readwrite');
        transaction.oncomplete = () => {
            resolve(true);
        };
        transaction.onerror = () => {
            reject(transaction.error);
        };
        transaction.objectStore(storeName).put(value);
    });
}

const webUtils = {
    base64ToBytes,
    bytesToBase64,
    openDb,
    idbGet,
    idbPut,
};

export = webUtils;
