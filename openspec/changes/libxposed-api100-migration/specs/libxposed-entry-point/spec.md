## ADDED Requirements

### Requirement: Module entry via META-INF java_init.list

The module SHALL declare its entry point class in `xposed/src/main/resources/META-INF/xposed/java_init.list` containing the fully qualified class name of the `XposedModule` subclass.

#### Scenario: Module loads in target process

- **WHEN** LSPosed framework loads the module into a target app process
- **THEN** it reads `META-INF/xposed/java_init.list`, instantiates `XposedEntry(base, param)`, and calls `onPackageLoaded()`

#### Scenario: Module loads in system_server

- **WHEN** LSPosed framework loads the module into system_server
- **THEN** it calls `onSystemServerLoaded()` on the `XposedEntry` instance for AIDL service registration

### Requirement: Module metadata via module.prop

The module SHALL declare `META-INF/xposed/module.prop` with `minApiVersion=100`, `targetApiVersion=100`, and `staticScope=false`.

#### Scenario: LSPosed reads module metadata

- **WHEN** LSPosed Manager scans installed modules
- **THEN** it reads `module.prop` and shows the module as requiring API level 100

### Requirement: XposedEntry extends XposedModule

The entry class SHALL extend `io.github.libxposed.api.XposedModule` and implement `onPackageLoaded(PackageLoadedParam)` and `onSystemServerLoaded(SystemServerLoadedParam)`.

#### Scenario: Constructor initialization

- **WHEN** LSPosed creates a new `XposedEntry` instance in a target process
- **THEN** the constructor receives `XposedInterface` (hook engine) and `ModuleLoadedParam` (process metadata), and sets a singleton reference

#### Scenario: Skip self-package

- **WHEN** `onPackageLoaded` fires for the module's own package (`com.astrixforge.devicemasker`)
- **THEN** no hooks are registered (module app is never hooked)

#### Scenario: Skip critical system processes

- **WHEN** `onPackageLoaded` fires for `com.android.systemui`, `com.android.phone`, or `com.google.android.gms`
- **THEN** no hooks are registered

### Requirement: Legacy xposed_init removed

The file `xposed/src/main/assets/xposed_init` SHALL be deleted. The module SHALL NOT contain legacy API 82 entry point declarations.

#### Scenario: APK does not contain legacy entry

- **WHEN** the release APK is built
- **THEN** `assets/xposed_init` does NOT exist in the APK, and `META-INF/xposed/java_init.list` DOES exist

### Requirement: Legacy dependencies removed

YukiHookAPI (`yukihookapi-api`, `yukihookapi-ksp`), KavaRef (`kavaref-core`, `kavaref-extension`), and legacy `de.robv.android.xposed:api:82` SHALL be removed from all Gradle configurations.

#### Scenario: Clean dependency tree

- **WHEN** `./gradlew :xposed:dependencies` is run
- **THEN** no `yukihookapi`, `kavaref`, or `de.robv.android.xposed` artifacts appear in the dependency tree
