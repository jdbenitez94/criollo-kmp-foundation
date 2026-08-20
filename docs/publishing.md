# Publishing

Publishing targets [Maven Central Publisher Portal](https://central.sonatype.com/) via the
[OSSRH Staging API compatibility endpoint](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/),
with in-memory PGP signing in CI.

## Coordinates

- Group: `io.github.jdbenitez94.criollo.kmp.foundation`
- Namespace (Portal): `io.github.jdbenitez94` (GitHub username namespace)
- Version: `ProjectConfig.version` in build-logic (must match git tag `vX.Y.Z` and `version.txt`)

## Release flow (automated)

| Branch | Behavior |
|--------|----------|
| `dev` | CI only — no tags, no Maven publish |
| `main` | [release-please](../.github/workflows/release-please.yml) opens a release PR from conventional commits |

1. Merge work to `main` (via PR). Prefer [Conventional Commits](commit-conventions.md).
2. release-please opens/updates a PR that bumps `version.txt`, `ProjectConfig.version`,
   `.release-please-manifest.json`, and `CHANGELOG.md`.
3. Merge that release PR → release-please creates tag `vX.Y.Z` + GitHub Release.
4. The same workflow run publishes to Maven Central (see
   [`publish-maven-central.yml`](../.github/workflows/publish-maven-central.yml)).
5. CI **Finalize Central Portal deployment** validates the upload and exposes it in the Portal
   (`publishing_type=user_managed`). When that step is green, publish (or drop) the deployment at
   <https://central.sonatype.com/publishing>.

Bootstrap: `initial-version` is `0.1.0` so the first release PR is `0.1.0`. After that, SemVer follows commit types.

### Manual tag (escape hatch)

```bash
git tag v0.1.0 && git push origin v0.1.0
```

[`release.yml`](../.github/workflows/release.yml) publishes on `v*` tag pushes and creates a
GitHub Release only if one does not already exist.

## Local smoke

```bash
./gradlew publishToMavenLocal
```

Artifacts land under `~/.m2/repository/io/github/jdbenitez94/criollo/kmp/foundation/`,
including:

- `bom` (Java Platform / dependencyManagement for all modules)
- library modules (`coroutines`, `coroutines-compose`, `coroutines-viewmodel` — Gradle `:coroutines`, `:coroutines:compose`, `:coroutines:viewmodel`)
- `project-conventions` Gradle plugin (+ plugin-marker POM for id
  `io.github.jdbenitez94.criollo.kmp.foundation.project-conventions`)

## GPG signing key

Key material lives in the machine keyring and gitignored files under `config/`
(`**/*.gpg` is ignored). Public key: [`config/public-key.asc`](../config/public-key.asc).

Create a project signing key the same way as Saveable (RSA-4096, comment = repo name):

```bash
gpg --batch --pinentry-mode loopback --passphrase-file <(openssl rand -base64 32) --gen-key <<'EOF'
Key-Type: RSA
Key-Length: 4096
Key-Usage: sign,cert
Subkey-Type: RSA
Subkey-Length: 4096
Subkey-Usage: encrypt
Name-Real: Joaquin Daniel Benitez
Name-Comment: criollo-kmp-foundation
Name-Email: jdbenitez94@gmail.com
Expire-Date: 0
%commit
EOF

FPR="$(gpg --list-secret-keys --with-colons 'criollo-kmp-foundation' | awk -F: '/^fpr:/ {print $10; exit}')"
gpg --export-secret-keys "$FPR" > config/secret-key-ring.gpg
gpg --export --armor "$FPR" > config/public-key.asc
# CI needs ASCII-armored secret: gpg --export-secret-keys --armor "$FPR"
```

Upload the public key (once) so Central can verify signatures, e.g. <https://keys.openpgp.org/>

## Secrets and variables

GitHub secrets: `OSSRH_USERNAME`, `OSSRH_PASSWORD` (Central Portal **user token**),
`SIGNING_KEY_ID`, `SIGNING_KEY`, `SIGNING_PASSWORD`.

GitHub variable: `MAVEN_CENTRAL_NAMESPACE` = `io.github.jdbenitez94`.

## Local secret store

All publish/signing secrets for this machine live in gitignored `local.properties`
(mode `0600`), including the ASCII-armored `signingInMemoryKey`. The convention plugin
reads Gradle/`-P`/env first, then falls back to that file.

Also mirrored (gitignored): `config/secret-key-ring.gpg`, `config/signing-secret.key`.
Public key: `config/public-key.asc`.

Omit signing props / set `signing.required=false` for unsigned `publishToMavenLocal`.

To rotate the **Central Portal user token**, generate a new one at
<https://central.sonatype.com/usertoken>, replace `mavenCentralUsername` /
`mavenCentralPassword` in `local.properties`, then ask the agent to resync secrets (or):

```bash
awk -F= '/^mavenCentralUsername=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set OSSRH_USERNAME -R jdbenitez94/criollo-kmp-foundation
awk -F= '/^mavenCentralPassword=/{print substr($0,index($0,"=")+1)}' local.properties \
  | gh secret set OSSRH_PASSWORD -R jdbenitez94/criollo-kmp-foundation
```
