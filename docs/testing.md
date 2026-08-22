# Testing

Foundation aligns JVM unit tests with saveable’s JUnit 5 stack.

## Rules of thumb

| Source set | Framework | Notes |
| ------------ | ----------- | ------- |
| `commonTest` | `kotlin.test` | Multiplatform (JS / Wasm / Apple / Android) |
| `jvmTest` / `androidHostTest` | JUnit Jupiter (`org.junit.jupiter`) | Nested suites, extensions, params |

Do not mix `kotlin.test.Test` with `useJUnitPlatform()` on JVM — Jupiter will not discover those
methods. Assertions from `kotlin.test` remain fine as plain functions.

## Monorepo wiring

`criollo.kmp-library` applies `convention.junit5`, which:

- Adds Jupiter + params to `jvmTest` / `androidHostTest`
- Adds `junit-platform-launcher` on the matching `*RuntimeOnly` configurations
- Sets every `Test` task to `useJUnitPlatform()`

Kotlin JVM modules (e.g. `:testing`, `:project-conventions`) apply `convention.junit5` directly.

## Nested layout

Group related cases with `@Nested` + `inner class` so IDE / CI reports stay readable:

```kotlin
class TaskScopeTest {

    @Nested
    inner class SkipIfActive {
        @Test
        fun skipIfActive_returnsSkipped() = runTest { /* … */ }
    }

    @Nested
    inner class ReplaceActive {
        @Test
        fun replaceActive_cancelsPrevious() = runTest { /* … */ }
    }
}
```

Shared helpers stay on the outer class and remain visible to nested suites.

## Published helpers (`testing`)

Maven: `io.github.jdbenitez94.criollo.kmp.foundation:testing`

```kotlin
testImplementation(platform("io.github.jdbenitez94.criollo.kmp.foundation:bom:<version>"))
testImplementation("io.github.jdbenitez94.criollo.kmp.foundation:testing")
```

### `MainTestDispatcherExtension`

Installs `Dispatchers.Main` with a `TestDispatcher` for each test method (ViewModel / UI code that
calls `Dispatchers.Main`):

```kotlin
@MainTestDispatcher
class SignInViewModelTest {
    @Test
    fun loads() = runTest {
        // Dispatchers.Main is a test dispatcher
    }
}
```

Or register the extension as a field when you need the concrete dispatcher:

```kotlin
class SignInViewModelTest {
    @RegisterExtension
    @JvmField
    val main = MainTestDispatcherExtension()

    @Test
    fun loads() = runTest(main.dispatcher) { /* … */ }
}
```

Package: `io.github.jdbenitez94.criollo.kmp.foundation.testing`.
