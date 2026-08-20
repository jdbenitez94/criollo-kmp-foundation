# Security Policy

## Supported versions

Security fixes are published for the latest SemVer release line on Maven Central.

| Version | Supported |
| ------- | --------- |
| 0.1.x   | :white_check_mark: |
| < 0.1   | :x: (unreleased / pre-Central) |

While the project is on `0.y.z`, breaking changes may land in minor bumps. Security
patches are backported only to the current supported line above unless a release is
explicitly marked LTS later.

## Reporting a vulnerability

**Do not open a public GitHub issue for security reports.**

1. Use [GitHub Private Vulnerability Reporting](https://github.com/jdbenitez94/criollo-kmp-foundation/security/advisories/new)
   on this repository (preferred), **or**
2. Email the maintainer at **<jdbenitez94@gmail.com>** with subject
   `[SECURITY] criollo-kmp-foundation`.

Please include:

- Affected artifact(s) and version(s) (`coroutines`, adapters, BOM, plugin, etc.)
- Description and impact (confidentiality / integrity / availability)
- Reproduction steps or a minimal proof of concept
- Whether you are willing to be credited in the advisory

### What to expect

| Stage | Target |
| ----- | ------ |
| Acknowledgement | within **72 hours** |
| Initial triage (accepted / needs info / declined) | within **7 days** |
| Fix or mitigation plan for accepted reports | as soon as practical; critical issues prioritized |

If a report is **accepted**, we will coordinate a fix, prepare a GitHub Security Advisory
when appropriate, and publish a patched release to Maven Central. If **declined**, we will
explain why (e.g. out of scope, not reproducible, or accepted risk).

## Scope

In scope: vulnerabilities in published library code, the `project-conventions` Gradle
plugin, build/publish configuration that could compromise consumers, and dependency
issues we can mitigate in this repo.

Out of scope: issues solely in third-party dependencies with no practical mitigation
here (we track those via Dependabot / OWASP Dependency Check / CodeQL), social
engineering, and denial-of-service against GitHub or Maven Central infrastructure.

## Supply chain

Releases are signed for Maven Central, CI runs static analysis and dependency scanning,
and version tags are produced through the documented release process. Prefer consuming
artifacts from Maven Central (or the BOM) rather than untrusted forks or snapshots.
