# Changelog

## Unreleased

### Features

* **kryptostore:** add `kryptostore-crypto` module (Phase A extract from saveable)
* **kryptostore:** add `kryptostore-serializers` with envelope + fail-closed (Phase B)
* **kryptostore:** add core factories + IndexedDB storage (Phase C)
* **kryptostore:** add `kryptostore-preferences` encrypted + plain prefs (Phase D)
* **kryptostore:** StoreRegistry re-encrypt, `kryptostore-android` delegates, StreamingAead (Phase E)
* **kryptostore:** BOM DataStore/Tink constraints, migrate-android, compat fixtures, ABI dumps (Phase F)
* **kryptostore:** publish DX artifact as `kryptostore-android-delegates` (avoid clash with KMP `*-android` target)
* **kryptostore:** Phase G — saveable dogfoods kryptostore (crypto/serializers/core); CRYPTO_KMP.md corrected

## [0.1.9](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.8...v0.1.9) (2026-08-22)

### Bug Fixes

* **publish:** upload atomic Portal release bundles ([#37](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/37)) ([a1873eb](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/a1873ebe419e6e05f3e0f7aa9c704f32064067f8))

## [0.1.8](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.7...v0.1.8) (2026-08-22)

### Bug Fixes

* **publish:** gate releases on full Central set ([#34](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/34)) ([6cc2c91](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/6cc2c9167a1af9ec3caa323a4c24480ec0971bfa))
* **release:** repair release-please workflow YAML ([#35](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/35)) ([5b2ffbd](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/5b2ffbdd5963cd05efb4654f565d87ea77b9534d))

## [0.1.7](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.6...v0.1.7) (2026-08-22)

### Bug Fixes

* **publish:** auto-publish full Central deployments ([#31](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/31)) ([f1dc417](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/f1dc417070013cb8b7b1af1aabf5b8aeebfd0a4a))

## [0.1.6](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.5...v0.1.6) (2026-08-22)

### Bug Fixes

* **publish:** include iOS klibs on Maven Central ([#27](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/27)) ([db66088](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/db660889283c50abc745cbfba826577f35fb4b3c))

## [0.1.5](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.4...v0.1.5) (2026-08-21)

### Features

* **ci:** hard ruleset sync via Deploy Key bypass ([#26](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/26)) ([bfb219b](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/bfb219b0b8abd3946b998be433939ba0ad2761ab))
* **ci:** snapshots from dev and keep tips aligned ([#23](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/23)) ([57c4b84](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/57c4b84dcbecc4d1166891d6d6ae98f503ec15a4))

## [0.1.4](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.3...v0.1.4) (2026-08-21)

### Features

* **build:** run Detekt on build-logic ([#20](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/20)) ([9ed9374](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/9ed9374811fe318069f74eaba5e0e9f70263b989))

## [0.1.3](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.2...v0.1.3) (2026-08-21)

### Features

* **build:** add local cloud parity and clear jscpd clones ([#18](https://github.com/jdbenitez94/criollo-kmp-foundation/issues/18)) ([5f163b3](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/5f163b3f81fdaf5efb651eca836897ecf060ea06))

## [0.1.2](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.1...v0.1.2) (2026-08-21)

### Bug Fixes

* clean CHANGELOG and map Codacy Kover as Kotlin ([2d60892](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/2d60892c113ba6dd8414763e1021524593d6c978))

## [0.1.1](https://github.com/jdbenitez94/criollo-kmp-foundation/compare/v0.1.0...v0.1.1) (2026-08-20)

### Bug Fixes

* **publish:** javadoc jars, finalize CI, README badges ([0df7a54](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/0df7a5482ce14ef8cf2bc58885b02e7bc5288112))

## 0.1.0 (2026-08-20)

### Features

* initial Criollo KMP Foundation release (0.1.0) ([e892102](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/e892102e02785b66c544e5d7728f12870ef99f21))

### Bug Fixes

* **security:** bump Kotlin and patch Jackson ([d58b182](https://github.com/jdbenitez94/criollo-kmp-foundation/commit/d58b182dbb9e20ea3050cd956be9781684f53d43))
