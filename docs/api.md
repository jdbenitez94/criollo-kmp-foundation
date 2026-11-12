# API reference

HTML API docs are generated with [Dokka](https://github.com/Kotlin/dokka) from the published
library modules (`coroutines`, `coroutines-compose`, `coroutines-viewmodel`, `result`, `runtime`,
`testing`, `kryptostore`, `kryptostore-crypto`, `kryptostore-serializers`,
`kryptostore-preferences`, `kryptostore-android-delegates`, and `kryptostore-migrate-android`).

On GitHub Pages they are published next to this site at
[jdbenitez94.github.io/criollo-kmp-foundation/api/](https://jdbenitez94.github.io/criollo-kmp-foundation/api/).

Locally:

```bash
./gradlew dokkaGenerateHtml
```

Output: `build/dokka/html`.
