# Security Policy

## Supported versions

Security fixes are published for the latest SemVer release line on Maven Central.

| Version | Supported |
| ------- | --------- |
| 0.1.x   | :white_check_mark: |
| < 0.1   | :x: (unreleased / pre-Central) |

While the project is on `0.y.z`, breaking changes may land in minor bumps. Security
patches are backported only to the current supported line above unless a release is
explicitly marked Long-Term Support (LTS) later.

## Reporting a vulnerability

**Do not open a public GitHub issue for security reports.**

Report through [GitHub Private Vulnerability Reporting](https://github.com/jdbenitez94/criollo-kmp-foundation/security/advisories/new)
on this repository. GitHub notifies maintainers and supports coordinated disclosure.

Please include:

- Maven coordinates and version(s) affected, for example
  `io.github.jdbenitez94.criollo.kmp.foundation:coroutines:0.1.0`,
  `:coroutines-compose`, `:coroutines-viewmodel`, `:bom`, or `:project-conventions`
- Description and impact (confidentiality / integrity / availability)
- Reproduction steps or a minimal proof of concept
- Whether you are willing to be credited in the advisory

### What to expect

| Stage | Target |
| ----- | ------ |
| Acknowledgement | within **72 hours** |
| Initial triage (accepted / needs info / declined) | within **7 days** |
| Fix or mitigation plan for accepted reports | as soon as practical; critical issues prioritized |

If a report is **accepted** (reproducible, in scope, and not a duplicate), we will
coordinate a fix, open a GitHub Security Advisory for issues with Common Vulnerability
Scoring System (CVSS) **≥ 4.0** or exploitable impact in default library usage, and
publish a semantic-versioning patch release to Maven Central within **14 days** of
merging the fix (within **7 days** for critical issues). If **declined**, we will
explain why within the triage window (for example out of scope, not reproducible, or
accepted risk documented in `config/owasp/suppressions.xml`).

## Scope

In scope: vulnerabilities in published library code, the `project-conventions` Gradle
plugin, build/publish configuration that could compromise consumers, and dependency
issues we can mitigate in this repo.

Out of scope: issues solely in third-party dependencies with no practical mitigation
here (we track those via Dependabot, Open Worldwide Application Security Project
(OWASP) Dependency Check, and GitHub CodeQL), social engineering, and
denial-of-service against GitHub or Maven Central infrastructure.

## Supply chain

Releases are signed for Maven Central, CI runs static analysis and dependency scanning,
and version tags are produced through the documented release process. Prefer consuming
artifacts from Maven Central (or the Bill of Materials (BOM)) rather than untrusted
forks or snapshots.
