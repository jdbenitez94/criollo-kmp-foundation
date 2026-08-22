import cryptoWorkerModule = require('./crypto-worker');

const cryptoWorkerScript = cryptoWorkerModule.cryptoWorkerScript;

type WorkerRequestType = 'ensureKey' | 'encrypt' | 'decrypt';

interface WorkerRequest {
    requestId: string;
    type: WorkerRequestType;
    keyAlias: string;
    plaintextBase64?: string;
    ciphertextBase64?: string;
    associatedDataBase64?: string | null;
}

interface WorkerResponse {
    requestId: string;
    ok: boolean;
    result?: unknown;
    error?: string;
}

const WORKER_REQUEST_TIMEOUT_MS = 15_000;

let cryptoWorker: Worker | null = null;
let cryptoWorkerUrl: string | null = null;
let nextRequestId = 0;
const pendingRequests = new Map<
    string,
    { resolve: (value: unknown) => void; reject: (reason: Error) => void; timer: ReturnType<typeof setTimeout> }
>();

function rejectAllPending(reason: Error): void {
    pendingRequests.forEach(({ reject, timer }) => {
        clearTimeout(timer);
        reject(reason);
    });
    pendingRequests.clear();
}

function terminateCryptoWorker(): void {
    if (cryptoWorker != null) {
        cryptoWorker.terminate();
        cryptoWorker = null;
    }
    if (cryptoWorkerUrl != null) {
        URL.revokeObjectURL(cryptoWorkerUrl);
        cryptoWorkerUrl = null;
    }
}

function ensureCryptoWorker(): Worker {
    if (cryptoWorker != null) {
        return cryptoWorker;
    }
    const blob = new Blob([cryptoWorkerScript], { type: 'application/javascript' });
    cryptoWorkerUrl = URL.createObjectURL(blob);
    cryptoWorker = new Worker(cryptoWorkerUrl);
    cryptoWorker.onmessage = (event: MessageEvent<WorkerResponse>) => {
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

function postToWorker(request: Omit<WorkerRequest, 'requestId'>): Promise<unknown> {
    const worker = ensureCryptoWorker();
    const requestId = `crypto-${nextRequestId++}`;
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
            pendingRequests.delete(requestId);
            reject(new Error(`Crypto worker request timed out after ${WORKER_REQUEST_TIMEOUT_MS}ms.`));
        }, WORKER_REQUEST_TIMEOUT_MS);
        pendingRequests.set(requestId, { resolve, reject, timer });
        worker.postMessage({ ...request, requestId } satisfies WorkerRequest);
    });
}

function install(): void {
    const subtle = globalThis.crypto?.subtle;
    if (!subtle) {
        throw new Error(
            'WebCrypto (crypto.subtle) is unavailable. ' +
                'KryptoStore requires a secure context (HTTPS or localhost).',
        );
    }
    // Worker is created lazily on first crypto operation.
}

async function ensureKey(keyAlias: string): Promise<boolean> {
    const result = await postToWorker({ type: 'ensureKey', keyAlias });
    return Boolean(result);
}

async function encrypt(
    keyAlias: string,
    plaintextBase64: string,
    associatedDataBase64: string | null,
): Promise<string> {
    const result = await postToWorker({
        type: 'encrypt',
        keyAlias,
        plaintextBase64,
        associatedDataBase64,
    });
    return String(result);
}

async function decrypt(
    keyAlias: string,
    ciphertextBase64: string,
    associatedDataBase64: string | null,
): Promise<string> {
    const result = await postToWorker({
        type: 'decrypt',
        keyAlias,
        ciphertextBase64,
        associatedDataBase64,
    });
    return String(result);
}

const kryptoStoreCrypto = {
    install,
    ensureKey,
    encrypt,
    decrypt,
    /** Test / teardown helper — terminates the worker and revokes its blob URL. */
    dispose: terminateCryptoWorker,
};

export = kryptoStoreCrypto;
