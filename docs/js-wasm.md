# JS / Wasm webpack notes

## `webpack.config.d/resolve-fallback.js`

Modules that target **JS browser** and/or **WasmJS browser** may ship a `webpack.config.d/` directory. Kotlin/JS webpack merges every `*.js` file under that folder into the generated webpack config.

[`coroutines/webpack.config.d/resolve-fallback.js`](../coroutines/webpack.config.d/resolve-fallback.js) (also under `coroutines/compose` and `coroutines/viewmodel`) disables Node polyfills for `path` and `os`:

```js
config.resolve.fallback = {
    ...config.resolve.fallback,
    path: false,
    os: false,
};
```

### Why

Browser bundles must not resolve Node built-ins. Without these fallbacks, webpack can fail or pull incorrect polyfills when transitive code references `path` / `os`.

### When to copy for a new module

Add the same `webpack.config.d/resolve-fallback.js` when:

1. The module declares `js { browser() }` and/or `wasmJs { browser() }`, **and**
2. Building a browser distribution (or a consumer app that merges this library into its webpack graph) fails with missing `path` / `os` module errors.

Pure JVM / Android / iOS modules do **not** need this file.

### What we deliberately omitted

App-specific `ts-loader` / TypeScript path wiring belongs in the consumer’s own `webpack.config.d/`,
not in this foundation library.

### Consumer apps

If your CMP web app already configures webpack fallbacks, you may not need anything extra. If you see webpack errors about `path` or `os` after depending on these artifacts, add the same fallback snippet to the **app** `webpack.config.d/` (or rely on the library fragment if webpack merges dependency configs in your toolchain).
