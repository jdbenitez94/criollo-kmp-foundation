/**
 * Re-exports the classic-script worker body for Blob-URL instantiation.
 * Source of truth: crypto-worker-source.js (no nested template literal).
 */
const cryptoWorkerSource = require('./crypto-worker-source');

module.exports = {
    cryptoWorkerScript: cryptoWorkerSource.cryptoWorkerScript,
};
