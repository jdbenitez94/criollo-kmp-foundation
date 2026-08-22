# KryptoStore completion checklist (Phase G)

G1–G10 from `docs/spec-final-kryptostore.md` §1.3:

| ID | Status | Evidence |
| ---- | -------- | ---------- |
| G1 | Done | Published `kryptostore-*` under foundation group; saveable consumes via Maven Local 0.1.9 |
| G2 | Done | Android, JVM, iOS, JS, Wasm targets on kryptostore modules |
| G3 | Done | Encrypted proto + encrypted prefs + plain prefs factories |
| G4 | Done | IndexedDB / localStorage / WebCrypto / keys in IndexedDB (`app-crypto`) |
| G5 | Done | `allowPlaintextRead` default false; fail-closed corruption |
| G6 | Done | `StoreRegistry.reEncryptAll` on rotation |
| G7 | Done | android-delegates, BOM, BCV `api/`, compat fixtures |
| G8 | Done | `docs/kryptostore-migration.md` + `kryptostore-migrate-android` |
| G9 | Done | saveable composeApp thin consumer; remember-email plain prefs |
| G10 | Done | README, CRYPTO.md / kryptostore-crypto.md, MIGRATION, CHANGELOG; saveable CRYPTO_KMP.md corrected |

## Consumer soak

- Repo: `saveable` `@ 0.1.9` Maven Local (or Central when released)
- Artifacts: `kryptostore-crypto`, `kryptostore-serializers`, `kryptostore`, `kryptostore-preferences`
- No in-tree `Encrypted*Serializer` / `CryptoManager` implementations remain in composeApp

## Release

Ship via existing release-please / Maven Central workflows when the owner tags a release. No automatic Central publish from this phase.
