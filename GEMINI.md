<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# Device Masker - Project Context

## Overview
Device Masker is an advanced Android LSPosed/Xposed module designed to protect user privacy by spoofing device identifiers and hiding hook injection. It features a modern Material 3 Expressive UI and robust anti-detection mechanisms.

## Tech Stack
- **Language**: Kotlin 2.3.0 (Java 25)
- **Framework**: Android SDK 36 (Android 16 Target)
- **UI**: Jetpack Compose (BOM 2025.12.00) + Material 3 Expressive (1.5.0-alpha11)
- **Hooking**: YukiHookAPI 1.3.1 + LSPosed (API 82) + KavaRef
- **Architecture**: MVVM, Multi-Module Gradle (`:app`, `:xposed`, `:common`)


## Build/Lint/Test Commands

### Build Commands
```bash
./gradlew assembleDebug              # Build debug APK
./gradlew assembleRelease            # Build release APK (requires signing)
./gradlew installDebug               # Build and install to connected device
./gradlew :app:assembleDebug         # Build only :app module
./gradlew :common:assembleDebug      # Build only :common module
./gradlew :xposed:assembleDebug      # Build only :xposed module
```

### Lint & Format Commands
```bash
./gradlew spotlessApply              # Auto-format all Kotlin files (ktfmt)
./gradlew spotlessCheck              # Check formatting without fixing
./gradlew lint                       # Run Android lint on all modules
./gradlew :app:lint                  # Run lint on :app only
./gradlew :common:lint               # Run lint on :common only
```

---

## Project Structure

```
devicemasker/
├── :app      # Main application (UI + KSP entry, Compose MVVM)
├── :common   # Shared models, generators, SharedPrefsKeys (SINGLE SOURCE OF TRUTH)
└── :xposed   # Xposed hook logic (YukiHookAPI hookers)
```

---

## Code Style Guidelines

### Formatting
- **Formatter**: ktfmt 0.54 with kotlinlangStyle (run `./gradlew spotlessApply`)
- **Indentation**: 4 spaces (no tabs)
- **Line length**: ~100 characters preferred
- **Trailing whitespace**: None (auto-trimmed by Spotless)
- **End of file**: Single newline

### Imports
- No wildcard imports (`import foo.*`)
- Group order: Android, Compose, third-party, project modules
- Remove unused imports (handled by ktfmt)

### Naming Conventions
| Type | Convention | Example |
|------|------------|---------|
| Classes/Objects | PascalCase | `DeviceHooker`, `SpoofGroup` |
| Functions | camelCase | `getSpoofValue()`, `onHook()` |
| Properties | camelCase | `isEnabled`, `packageName` |
| Constants | SCREAMING_SNAKE | `KEY_MODULE_ENABLED`, `TAG` |
| Composables | PascalCase | `HomeScreen()`, `ExpressiveSwitch()` |
| State classes | PascalCase + State suffix | `HomeState`, `SettingsState` |
| ViewModels | PascalCase + ViewModel suffix | `HomeViewModel` |

### Types & Null Safety
- Prefer non-nullable types; use nullable only when semantically meaningful
- Use `?.let { }` or `?: return` for null handling
- Use `runCatching { }.getOrElse { }` for exception-prone operations in hooks
- Always specify explicit return types for public functions

### Data Classes
- Use `@Serializable` for cross-process data models
- Prefer immutable `val` properties
- Use `copy()` for updates (immutable patterns)
- Place defaults in constructor parameters

### Error Handling
- **In Hookers**: Wrap uncertain methods with `runCatching { }` to prevent crashes
- Use `optional()` for methods that may not exist on all Android versions
- Log errors with `DualLog.warn(TAG, message, throwable)`
- Never crash target apps - fail gracefully with fallback values

### Compose Guidelines
- Use `collectAsStateWithLifecycle()` for StateFlow in UI
- Prefer `derivedStateOf` for expensive computations
- Add stable `key` to all `LazyColumn` items
- Use `MaterialTheme.colorScheme.*` - never hardcode colors
- Use `AppMotion.*` springs for animations

---

## Critical Safety Rules (Xposed)

1. **Never hook system-critical processes**: Skip `android`, `system_server`, `com.android.systemui`
2. **Load AntiDetectHooker FIRST**: Before any spoofing hooks
3. **Use `optional()` for uncertain methods**: Prevents crashes on different Android versions
4. **Never block essential class loading**: Allow `androidx.*`, `kotlin.*`, `java.*`, `android.*`

---

## Key Architecture Notes

- **SharedPrefsKeys** in `:common` is the SINGLE SOURCE OF TRUTH for preference keys
- **XSharedPreferences caches values** - config changes require target app restart
- **BaseSpoofHooker** provides shared functionality for all hookers
- **ConfigSync** bridges UI config (JsonConfig) to hooks (XposedPrefs)

---

## Documentation References

### Local Docs (Source of Truth)
- YukiHookAPI: @/docs/official-best-practices/lsposed/YukiHookAPI.md
- Kotlin 2.3.0: @/docs/official-best-practices/kotlin/kotlin-2-3-0-guide.md
- Material 3: @/docs/official-best-practices/material-ui/material-3-guide.md

### Online Resources
- [Compose Docs](https://developer.android.com/develop/ui/compose/documentation)
- [M3 API Reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [Compose Performance](https://developer.android.com/develop/ui/compose/performance/bestpractices)