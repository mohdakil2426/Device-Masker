# GitHub Actions Plan

Date: 2026-03-16
Project: Device Masker
Scope: Best-practice GitHub Actions design for this Android multi-module LSPosed project
Status: Planning only

## Objective

Design a GitHub Actions setup that is:

- secure by default
- fast enough for day-to-day PR use
- aligned with this repo's actual Gradle quality gates
- realistic for an Android + LSPosed module project
- maintainable as the project evolves

This plan intentionally separates routine CI from release automation and from GitHub dependency metadata submission.

## Project Constraints That Shape The Workflow Design

### Repo-specific constraints

- Multi-module Gradle project: `:app`, `:common`, `:xposed`
- Current required quality gate order:
  1. `spotlessApply` or `spotlessCheck`
  2. compile
  3. lint
  4. test
  5. `assembleDebug`
  6. `assembleRelease`
- Release signing uses environment variables:
  - `KEYSTORE_PATH`
  - `KEYSTORE_PASS`
  - `KEY_ALIAS`
  - `KEY_PASS`
- `gradle.properties` already enables:
  - build cache
  - configuration cache
  - parallel builds
  - 4 GB heap
- Current local helper script `scripts/run-audit.ps1` is Windows-oriented and is not ideal as the primary CI entrypoint

### Android/LSPosed-specific constraints

- GitHub-hosted runners are excellent for static validation, unit tests, lint, and APK builds
- GitHub-hosted runners are not a good fit for rooted-device LSPosed validation
- Rooted/system-server behavioral verification should stay manual or move to a separate device-lab strategy later
- CI should not try to "fake" rooted LSPosed runtime coverage on hosted runners

## Research-Based Recommendations

### 1. Use Ubuntu as the primary CI runner

Recommended default runner:

- `ubuntu-latest`

Why:

- simpler Gradle invocation with `./gradlew`
- avoids the PowerShell and `cmd.exe` launcher issues already seen locally
- cheaper and faster than macOS for this workload
- sufficient for formatting, compile, lint, unit test, debug build, and release build validation

### 2. Use modern official actions

Recommended action families:

- `actions/checkout`
- `actions/setup-java`
- `gradle/actions/setup-gradle`
- `actions/upload-artifact`

Current official references indicate:

- `actions/checkout` examples now show `@v6`
- `actions/setup-java` examples show `@v5`
- `gradle/actions/setup-gradle` examples show `@v5`
- `actions/upload-artifact` current major is `@v4` with newer runtime changes in `v6`

Implementation best practice:

- pin third-party and GitHub actions to full commit SHAs in the actual workflow files
- optionally keep the major version in comments beside the SHA for readability

### 3. Use least-privilege `GITHUB_TOKEN` permissions

Default workflow permission target:

```yaml
permissions:
  contents: read
```

Only elevate on workflows that truly need it, for example:

- dependency submission: `contents: write`
- release publishing: `contents: write`
- artifact attestations or OIDC only if later adopted

### 4. Use workflow concurrency

Recommended pattern:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Why:

- avoids wasting runner time on stale pushes
- gives faster PR feedback

### 5. Use `setup-gradle` instead of custom cache logic

Recommended:

- use `gradle/actions/setup-gradle` in every Gradle job
- do not hand-roll `actions/cache` for Gradle unless a measured gap appears

Why:

- official Gradle guidance recommends `setup-gradle` for optimal CI execution
- wrapper validation is already built into `setup-gradle` in modern versions
- simpler and safer than maintaining custom cache keys

### 6. Upload artifacts intentionally, not excessively

Recommended uploads:

- lint reports on failure and optionally on success
- test reports on failure and optionally on success
- debug APK from main branch and manual runs
- release APK/AAB only in release workflow

Recommended retention:

- PR validation artifacts: `7` days
- main branch build artifacts: `14` days
- release artifacts: rely on GitHub Release assets plus short workflow artifact retention

### 7. Keep dependency metadata separate

Recommended separate workflow:

- dependency submission on `push` to `main`

Why:

- clearer permissions boundary
- cleaner CI logs
- aligns with Gradle's dedicated dependency submission action

## Recommended Workflow Topology

Use three core workflows.

### Workflow 1: `ci.yml`

Purpose:

- required PR and push validation

Triggers:

```yaml
on:
  pull_request:
    branches: [main]
  push:
    branches: [main]
  workflow_dispatch:
```

Optional path filtering:

- ignore docs-only and memory-bank-only changes if desired
- do not ignore `.github/**`
- do not ignore build files, scripts, or Gradle wrapper files

Suggested ignored paths:

```yaml
paths-ignore:
  - "memory-bank/**"
  - "openspec/**"
  - ".agents/**"
  - ".claude/**"
  - "docs/archive/**"
```

Do not ignore all `docs/**` if planning docs should still be able to validate workflows later.

#### CI job structure

Recommended jobs:

1. `quality`
- checkout
- setup Java 17 with Temurin
- setup Gradle
- run:
  - `chmod +x gradlew`
  - `./gradlew spotlessCheck`
  - `./gradlew :app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin`
  - `./gradlew lint`
  - `./gradlew test`

2. `assemble-debug`
- depends on `quality`
- run `./gradlew assembleDebug`
- upload debug APK and key reports

3. `assemble-release-check`
- depends on `quality`
- run `./gradlew assembleRelease`
- only on `push` to `main` and `workflow_dispatch`
- validates release shrinker/proguard configuration without publishing

Why split release assembly from the basic PR path:

- PR feedback stays faster
- release validation still happens regularly on trusted branches

#### CI job settings

Recommended:

- `runs-on: ubuntu-latest`
- `timeout-minutes: 30` for quality
- `timeout-minutes: 20` for assemble jobs
- `permissions: { contents: read }`

#### CI artifact strategy

Upload:

- `app/build/reports/lint-results-debug.*`
- `common/build/reports/tests/**`
- `app/build/reports/tests/**`
- `app/build/outputs/apk/debug/*.apk`

Use:

- `if: always()` for reports
- `if-no-files-found: warn` or `error` depending on strictness

### Workflow 2: `dependency-submission.yml`

Purpose:

- submit Gradle dependency graph to GitHub dependency graph / Dependabot ecosystem

Trigger:

```yaml
on:
  push:
    branches: [main]
  workflow_dispatch:
```

Permissions:

```yaml
permissions:
  contents: write
```

Steps:

- checkout
- setup Java 17
- `gradle/actions/dependency-submission`

Why separate:

- narrow write permission scope
- easier debugging and policy management

### Workflow 3: `release.yml`

Purpose:

- build, sign, and publish release artifacts safely

Triggers:

```yaml
on:
  push:
    tags:
      - "v*"
  workflow_dispatch:
```

Environment:

- use a protected GitHub Environment named `release`
- require manual approval if desired

Permissions:

```yaml
permissions:
  contents: write
```

Core flow:

1. checkout
2. setup Java 17
3. setup Gradle
4. decode/import keystore from secrets
5. run:
   - `./gradlew spotlessCheck`
   - `./gradlew lint`
   - `./gradlew test`
   - `./gradlew assembleRelease`
6. upload release artifact
7. create or update GitHub Release and attach APK

Important:

- never expose signing secrets to PR workflows
- do not run release publishing on `pull_request`
- only trusted refs and manual dispatch should access signing material

## Optional Workflows

These are good later additions, but not required for the first rollout.

### Optional A: `nightly.yml`

Purpose:

- full scheduled validation
- slower checks without blocking contributors

Good candidates:

- `assembleRelease`
- dependency updates health check
- stricter lint review

### Optional B: `wrapper-validation.yml`

A standalone wrapper-validation workflow is optional.

Reason:

- `setup-gradle` already performs wrapper validation in modern versions

Add a standalone workflow only if you want:

- a dedicated required check for wrapper changes
- very fast failure on wrapper tampering

### Optional C: emulator or managed-device tests

Not recommended as phase 1 for this repo.

Reason:

- the highest-value runtime behavior here involves LSPosed/root/system-server paths
- hosted emulator success would still not prove the critical module behavior

If added later, keep it limited to pure app UI/instrumentation checks.

## Recommended YAML Design Principles

### Principle 1: Do not drive CI through `run-audit.ps1`

For GitHub Actions, prefer native commands in YAML:

```bash
./gradlew spotlessCheck
./gradlew :app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin
./gradlew lint
./gradlew test
./gradlew assembleDebug
```

Why:

- avoids Windows shell assumptions
- makes logs easier to inspect per step
- gives clearer GitHub annotations and timing

### Principle 2: Keep each step focused

Recommended step boundaries:

- setup
- format check
- compile
- lint
- tests
- debug assemble
- release assemble
- artifact upload

Why:

- better failure localization
- better cache reuse visibility

### Principle 3: Use stable Java explicitly

Recommended:

- Temurin JDK 17

Reason:

- matches project build scripts
- aligns with Android toolchain compatibility
- avoids drift from runner-default JDKs

### Principle 4: Prefer trusted branch release validation

Recommended:

- PRs run quality + debug build
- `main` runs quality + debug build + release build check
- tags/manual runs perform signed release publishing

### Principle 5: Short artifact retention

Recommended defaults:

- PR reports: 7 days
- main build artifacts: 14 days
- release run artifacts: 14 days

GitHub Releases should hold long-term release binaries instead of Actions artifacts.

## Secrets And Environment Plan

### Release environment secrets

Recommended GitHub Environment: `release`

Secrets:

- `KEYSTORE_BASE64` or equivalent
- `KEYSTORE_PASS`
- `KEY_ALIAS`
- `KEY_PASS`

Implementation note:

- prefer storing the keystore as base64 in a secret, decode it during the release job, then point `KEYSTORE_PATH` at the decoded file

Why:

- easier than storing a path
- portable across runners

### Non-secret variables

Possible repository variables:

- `JAVA_VERSION=17`
- `GRADLE_OPTS` if needed later

## Branch Protection Recommendations

Recommended required status checks on `main`:

- `quality`
- `assemble-debug`

Optional required checks:

- `assemble-release-check`
- `dependency-submission`

Recommended repository settings:

- require pull request before merge
- dismiss stale approvals on new commits
- require branches to be up to date before merge
- require actions to be pinned to a full-length commit SHA if your org policy allows it

## Proposed Implementation Order

### Phase 1: Core CI

Create:

- `.github/workflows/ci.yml`

Deliver:

- PR validation
- main branch validation
- debug APK artifacts
- lint and test report artifacts

### Phase 2: Dependency Graph

Create:

- `.github/workflows/dependency-submission.yml`

Deliver:

- GitHub dependency graph submission

### Phase 3: Release Automation

Create:

- `.github/workflows/release.yml`

Deliver:

- signed release build
- GitHub Release publishing

### Phase 4: Nice-to-have refinements

Possible additions:

- wrapper-only fast validation workflow
- nightly scheduled health workflow
- PR title / conventional commit workflow
- Dependabot for GitHub Actions and Gradle

## Risks And Mitigations

### Risk 1: Release signing secrets leak to PRs

Mitigation:

- never expose release secrets on `pull_request`
- keep signing in tag/manual release workflow only
- use protected environment for release

### Risk 2: CI becomes too slow

Mitigation:

- keep PR workflow focused on quality + debug assemble
- push release assemble to `main` and release workflows
- rely on `setup-gradle` caching and concurrency cancellation

### Risk 3: False confidence from hosted Android runtime tests

Mitigation:

- keep LSPosed/root verification outside hosted CI
- treat CI as static/build/unit validation first

### Risk 4: Action supply-chain drift

Mitigation:

- pin actions to full SHAs
- use official GitHub and Gradle actions where possible
- enable Dependabot updates for workflow actions later

## Final Recommendation

Best first implementation for this repository:

1. `ci.yml`
2. `dependency-submission.yml`
3. `release.yml`

Do not start with emulator-heavy or rooted-device workflows.

For this project, the highest-value GitHub Actions setup is:

- Ubuntu-based
- Gradle-native
- security-conscious
- split by responsibility
- fast on PRs
- stricter on `main`
- secret-aware for releases only

## Official References

GitHub Actions:

- Workflow syntax and permissions:
  - https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions
- Concurrency:
  - https://docs.github.com/en/actions/reference/workflow-syntax-for-github-actions
- Artifact retention:
  - https://docs.github.com/en/actions/tutorials/store-and-share-data
- Security hardening / pinning actions:
  - https://docs.github.com/en/enterprise-cloud@latest/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions
- Repository setting to require SHA-pinned actions:
  - https://docs.github.com/github/administering-a-repository/managing-repository-settings/configuring-the-retention-period-for-github-actions-artifacts-and-logs-in-your-repository

Official Actions / Gradle:

- `actions/checkout`:
  - https://github.com/actions/checkout
- `actions/setup-java`:
  - https://github.com/actions/setup-java
- `gradle/actions`:
  - https://github.com/gradle/actions
- Gradle wrapper validation best practice:
  - https://docs.gradle.org/current/userguide/best_practices_security.html

Android / Google:

- Android CI system guidance:
  - https://developer.android.com/training/testing/continuous-integration/features
- Android build performance guidance:
  - https://developer.android.com/build/optimize-your-build
