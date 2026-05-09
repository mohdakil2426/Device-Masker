# Android 16 Compatibility And DevCheck Crash Report

Date started: 2026-05-09
Status: active

## Purpose

Track real Android 16 behavior separately from Android 13 emulator behavior. Android 13 smoke passing is not enough to claim Android 16 stability.

## Device Matrix

| Device | Android | SDK | Page size | LSPosed | App build | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Android 13 emulator | 13 | 33 | 4096 from preflight | installed | local debug + unsigned local release/ciRelease builds | Gradle gate green; verifier probe renders `procMaps`, `packageVisibility`, and `runtime` |
| Real Android 16 device | 16 | 36 | collect with `adb shell getconf PAGE_SIZE` | collect from LSPosed Manager/logcat | collect from APK metadata | DevCheck crash evidence required |

## DevCheck Crash Evidence

| Run | Module state | App enabled | Hook policy | Crash? | Evidence files | Root cause |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | disabled in LSPosed | none | none | not run yet | pending A16 device run | no conclusion |
| 2 | enabled | app not configured | load-only | not run yet | pending A16 device run | no conclusion |
| 3 | enabled | enabled | anti-detect only | not run yet | pending A16 device run | no conclusion |
| 4 | enabled | enabled | device/system hooks only | not run yet | pending A16 device run | no conclusion |
| 5 | enabled | enabled | telephony/subscription only | not run yet | pending A16 device run | no conclusion |
| 6 | enabled | enabled | package/feature hooks only | not run yet | pending A16 device run | no conclusion |
| 7 | enabled | enabled | all safe hooks | not run yet | pending A16 device run | no conclusion |

## Root Cause Notes

- No conclusion until logcat/tombstone proves the crash class.
- If crash occurs with module disabled, it is not a Device Masker hook crash.
- If crash occurs with module enabled but app not configured, suspect module load or anti-detection.
- If crash appears only after a hook family is enabled, fix that hook family first.
- Hidden API, non-SDK, `NoSuchMethodError`, `NoSuchFieldError`, `VerifyError`, and `UnsatisfiedLinkError` signatures must be captured from logcat before claiming an Android 16 fix.

## Local Verification Completed

| Check | Result | Evidence |
| --- | --- | --- |
| Full Gradle gate | pass | `logs/build/2026-05-09-a16-proc-maps-final-gate.txt` |
| Android 13 verifier runtime | pass | `logs/device/2026-05-09-verifier-a13-final.json`, `logs/device/2026-05-09-verifier-a13-final-logcat.txt` |
| Mobile MCP verifier launch | pass | Android 13 emulator rendered JSON with `procMaps`, `packageVisibility`, and `runtime` sections |
| 16 KB debug APK check | pass | command output from `scripts/verify-16kb-page-support.ps1 app/build/outputs/apk/debug/app-debug.apk` |
| 16 KB release APK check | pass | `logs/build/2026-05-09-16kb-release.txt` |
| 16 KB ciRelease APK check | pass | `logs/build/2026-05-09-16kb-ci-release.txt` |

Current Android 13 verifier maps counts are unredacted baseline values because the verifier run was not scoped/configured through LSPosed. They prove the probes execute and record suspicious line counts; they do not claim redaction is active.

Local `release` and `ciRelease` APKs are unsigned because release signing environment variables are not present in this workspace. They were built and 16 KB checked, but not installed for runtime smoke.

## Optional Offline APK Analysis

DexKit was considered only as offline research tooling. It is not a `:xposed` runtime dependency.

| Target APK | Evidence searched | Result |
| --- | --- | --- |
| DevCheck | `/proc/self/maps`, PackageManager queries, Build reads, Telephony reads, Runtime.exit/System.exit | not run; use only if logcat/tombstones/Frida remain inconclusive |
