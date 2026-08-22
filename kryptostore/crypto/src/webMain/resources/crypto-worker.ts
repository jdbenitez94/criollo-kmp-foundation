/**
 * Re-exports the classic-script worker body for Blob-URL instantiation.
 * Source of truth: crypto-worker-source.ts (no nested template literal).
 */
import cryptoWorkerSource = require('./crypto-worker-source');

const cryptoWorkerModule = {
    cryptoWorkerScript: cryptoWorkerSource.cryptoWorkerScript,
};

export = cryptoWorkerModule;
