# KryptoStore dogfood — saveable checklist (REQ-SMP-*)

**Consumer repo:** `/Users/jbenitez/Projects/saveable` (sibling of this foundation monorepo).

This document is the foundation-side gate for REQ-SMP-01..04 before a serious 1.0 tag.
Work lands primarily in **saveable** PRs; keep this checklist updated here.

## Prerequisites

- [x] Foundation publishes usable artifacts (`publishToMavenLocal` or snapshot/Central).
- [x] saveable depends on the foundation BOM plus `kryptostore`, `kryptostore-preferences`,
  `kryptostore-crypto`, `kryptostore-serializers` (and android-delegates if needed).

## Migration steps

1. **Replace in-tree crypto/datastore** with kryptostore factories (`createEncryptedProtoDataStore`, prefs factories).
2. **Use `StoreLocator.platform(producePath, name)`** for shared commonMain stores; on Android prefer delegates or `KryptostoreAndroid.initialize` + `KryptostorePaths.file`.
3. **remember-email → plain prefs** (`createPlainPreferencesDataStore` / `plainPreferencesDataStore`) — never encrypted (REQ-SMP-02).
4. **Delete duplicated** `Encrypted*Serializer` / local `CryptoManager` copies from composeApp (REQ-SMP-01).
5. **Fix `CRYPTO_KMP.md`**: keys in IndexedDB (`app-crypto`), fail-closed default, AAD derive, rotation/re-encrypt (REQ-SMP-03).
6. **securePrefs**: either wire an intentional consumer or remove orphan Koin bindings (REQ-SMP-04).

## Verification

| REQ | Check |
| ----- | ------- |
| REQ-SMP-01 | `grep`/CI gate: no composeApp-owned Encrypted serializer implementations |
| REQ-SMP-02 | Logout → remember-email restored when flag set (test or manual) |
| REQ-SMP-03 | CRYPTO_KMP.md matches kryptostore-crypto.md / this foundation |
| REQ-SMP-04 | No dead securePrefs wiring without comment |

## Owner tracking

| Item | Status |
| ------ | -------- |
| saveable consumes foundation kryptostore | Done @ **0.1.9** Maven Local / BOM |
| CRYPTO_KMP.md corrected for IndexedDB / fail-closed / AAD | Done @ 0.1.9 |
| Soak tip (StoreLocator / web multi-key) after next 0.2.x publish | _pending_ |
| Foundation tag candidate after soak | Stay on **0.2.x** (no 1.0 yet) |
