# Pull Request

## Description

<!-- What does this PR change and why? -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation
- [ ] Refactoring
- [ ] Tests
- [ ] Build / CI

## Related issues

- Closes #
- Relates to #

## Testing

- [ ] `./gradlew qualityCheck` (Detekt, KtLint, Kover; installs git hooks)
- [ ] JVM tests for touched modules (`./gradlew jvmLibraryTests` or module `jvmTest`)
- [ ] No new entries in `config/detekt/baseline.xml` without justification

## Checklist

### Code quality

- [ ] Detekt passes (`./gradlew detekt`)
- [ ] KtLint passes (`./gradlew ktlintCheck`)
- [ ] Kover threshold met (`./gradlew koverVerify`) when library code changed
- [ ] KMP source sets considered (commonMain, platform expect/actual as applicable)

### Documentation

- [ ] Updated `config/README.md`, `docs/`, `README.md`, or `CHANGELOG.md` if behavior or coordinates changed

## Notes for reviewers

<!-- Optional focus areas -->
