# Design: Rebrand to Device Masker

## Context

The project is undergoing a complete rebranding from PrivacyShield to Device Masker. This affects:
- Public-facing app identity (name, package)
- Developer attribution
- Internal code organization (package structure)
- All documentation and configuration

### Constraints
- Must maintain all existing functionality
- Must not introduce new bugs or regressions
- Build must succeed after all changes
- LSPosed module must work correctly after reinstall

### Stakeholders
- End users (will need to reinstall)
- LSPosed framework (module re-registration required)
- Developer (new identity: AstrixForge)

## Goals / Non-Goals

### Goals
- Complete removal of all "PrivacyShield", "akil", and "AKIL" references
- Establish "Device Masker", "AstrixForge", and `com.astrixforge.devicemasker` identity
- Maintain functional parity with existing implementation
- Clean, verifiable build after rebranding

### Non-Goals
- Feature additions or changes
- Architecture modifications
- Performance optimizations
- New UI elements or flows

## Decisions

### Decision 1: Directory Rename Approach
**What**: Physically move the entire package directory structure, then update all references.

**Why**: This is cleaner than creating new directories and copying files. It preserves git history if done carefully.

**Alternatives considered**:
1. ❌ Create new package, copy files, delete old - More error-prone, loses git history
2. ✅ Rename directories in place, update references - Cleaner, maintains structure

### Decision 2: Execution Order
**What**: Execute changes in this specific order:
1. Clean build artifacts first
2. Rename package directories
3. Update Gradle configuration
4. Rename class files
5. Update package declarations and imports
6. Update resources
7. Update documentation
8. Final clean build

**Why**: This order minimizes intermediate broken states and allows for verification at each step.

### Decision 3: Anti-Detection Update Priority
**What**: Update `AntiDetectHooker.kt` hidden patterns immediately after package rename.

**Why**: The anti-detection module must hide the new package name to prevent detection. This is a security-critical change that must not be forgotten.

### Decision 4: Documentation Scope
**What**: Update ALL documentation files, including memory-bank, OpenSpec, and README.

**Why**: Complete rebranding means no traces of old identity should remain. Partial updates create confusion.

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Build failures after partial rename | High | Follow strict execution order, verify after each phase |
| Missing package reference | Medium | Use grep/ripgrep to find ALL references before and after |
| LSPosed not detecting new module | Medium | Document re-registration steps clearly |
| User data loss | Low | Document this as expected behavior, users can regenerate |
| Anti-detection bypass | High | Update hidden patterns immediately, test with detection apps |

## Migration Plan

### Pre-Migration
1. Ensure current build is clean and working
2. Create backup or git commit of current state
3. Verify all existing tests pass

### Migration Steps
See `tasks.md` for detailed step-by-step implementation.

### Post-Migration
1. Run `./gradlew clean build`
2. Install APK on test device
3. Re-enable module in LSPosed Manager
4. Verify hook functionality
5. Test anti-detection with RootBeer or similar

### Rollback
If migration fails:
1. Git reset to pre-migration commit
2. Clean build artifacts
3. Rebuild

## Open Questions

None - this is a straightforward rebranding operation with well-defined scope.

## File Change Summary

### Kotlin Source Files (44 total)
All files in `app/src/main/kotlin/com/akil/privacyshield/` need:
- Directory path change to `com/astrixforge/devicemasker/`
- Package declaration update
- Import statement updates

### Gradle Files (2 files)
- `app/build.gradle.kts`: namespace, applicationId
- `settings.gradle.kts`: rootProject.name

### Android Resources (4+ files)
- `AndroidManifest.xml`: application class, theme
- `strings.xml`: app_name, xposed_description
- `themes.xml`: Theme name
- `values-night/themes.xml`: Theme name

### Documentation (15+ files)
- `README.md`
- `GEMINI.md`
- `openspec/project.md`
- All 6 memory-bank files
- Various OpenSpec change documents
