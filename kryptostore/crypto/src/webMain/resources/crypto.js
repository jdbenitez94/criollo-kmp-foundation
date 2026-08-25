const cryptoWorkerModule = require('./crypto-worker');

const cryptoWorkerScript = cryptoWorkerModule.cryptoWorkerScript;

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
            pending.reject(new Error(error ?? 'Crypto worker request failed.'));
        }
    };
    cryptoWorker.onerror = (event) => {
        rejectAllPending(new Error(event.message ?? 'Crypto worker crashed.'));
        terminateCryptoWorker();
    };
    return cryptoWorker;
}

function postToWorker(request) {
    const worker = ensureCryptoWorker();
    const requestId = `crypto-${nextRequestId++}`;
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
            pendingRequests.delete(requestId);
            reject(new Error(`Crypto worker request timed out after ${WORKER_REQUEST_TIMEOUT_MS}ms.`));
        }, WORKER_REQUEST_TIMEOUT_MS);
        pendingRequests.set(requestId, { resolve, reject, timer });
        worker.postMessage(Object.assign({}, request, { requestId }));
    });
}

function install() {
    const subtle = globalThis.crypto?.subtle;
    if (!subtle) {
        throw new Error(
            'WebCrypto (crypto.subtle) is unavailable. ' +
                'KryptoStore requires a secure context (HTTPS or localhost).',
        );
    }
}

function ensureKeyring(appId) {
    return postToWorker({ type: 'ensureKeyring', appId }).then(Boolean);
}

function encrypt(appId, plaintextBase64, associatedDataBase64) {
    return postToWorker({
        type: 'encrypt',
        appId,
        plaintextBase64,
        associatedDataBase64,
    }).then(String);
}

function decrypt(appId, ciphertextBase64, associatedDataBase64) {
    return postToWorker({
        type: 'decrypt',
        appId,
        ciphertextBase64,
        associatedDataBase64,
    }).then(String);
}

function rotateIfNeeded(appId, periodMs, nowMillis) {
    return postToWorker({
        type: 'rotateIfNeeded',
        appId,
        periodMs,
        nowMillis,
    }).then(Boolean);
}

function listKeyIds(appId) {
    return postToWorker({ type: 'listKeyIds', appId }).then(String);
}

function deleteKey(appId, keyId) {
    return postToWorker({ type: 'deleteKey', appId, keyId }).then(Boolean);
}

function getActiveKeyId(appId) {
    return postToWorker({ type: 'getActiveKeyId', appId }).then(String);
}

module.exports = {
    install,
    ensureKeyring,
    encrypt,
    decrypt,
    rotateIfNeeded,
    listKeyIds,
    deleteKey,
    getActiveKeyId,
    dispose: terminateCryptoWorker,
};
