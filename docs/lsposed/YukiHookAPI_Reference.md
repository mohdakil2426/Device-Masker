# YukiHookAPI Complete Reference Guide

## Table of Contents
1. [What is YukiHookAPI](#what-is-yukihookapi)
2. [Why Use YukiHookAPI](#why-use-yukihookapi)
3. [Project Setup](#project-setup)
4. [Core Concepts](#core-concepts)
5. [Hook Entry Point](#hook-entry-point)
6. [Method Hooking](#method-hooking)
7. [Field Access](#field-access)
8. [Class Loading](#class-loading)
9. [Error Handling](#error-handling)
10. [Best Practices](#best-practices)
11. [Common Mistakes](#common-mistakes)
12. [Migration from Xposed API](#migration-from-xposed-api)

---

## What is YukiHookAPI

**YukiHookAPI** is an efficient Hook API and Xposed Module solution built in Kotlin. It provides:

- **High-level Kotlin DSL** for writing hooks elegantly
- **Automatic entry point generation** (no manual xposed_init file management)
- **Powerful reflection utilities** via KavaRef integration
- **Type-safe method/field finders** with intuitive lambda syntax
- **Built-in logging and debugging** support
- **Comprehensive error handling** with detailed exception messages

### Key Components

| Component | Purpose |
|-----------|---------|
| `YukiHookAPI` | Main API entry point |
| `YukiBaseHooker` | Base class for modular hookers |
| `PackageParam` | Context for hook operations |
| `HookParam` | Access to method/constructor parameters and results |
| `KavaRef` | Reflection API (since v1.3.0) |

---

## Why Use YukiHookAPI

### Comparison: YukiHookAPI vs Raw Xposed API

#### Raw Xposed API (Verbose, Error-prone)
```java
public class HookEntry implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XModuleResources.createInstance(startupParam.modulePath, null);
    }
    
    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.android.browser")) {
            XposedHelpers.findAndHookMethod(
                Activity.class.getName(),
                lpparam.classLoader,
                "onCreate",
                Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // Your code here
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        // Your code here
                    }
                }
            );
        }
    }
}
```

#### YukiHookAPI (Elegant, Type-safe)
```kotlin
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    
    override fun onHook() = encase {
        loadApp(name = "com.android.browser") {
            Activity::class.resolve().firstMethod {
                name = "onCreate"
                parameters(Bundle::class)
            }.hook {
                before { /* Your code here */ }
                after { /* Your code here */ }
            }
        }
    }
}
```

### Benefits Summary

| Feature | Raw Xposed | YukiHookAPI |
|---------|------------|-------------|
| Entry point | Manual xposed_init | Auto-generated |
| Language | Java-centric | Kotlin-native |
| Syntax | Verbose callbacks | Lambda DSL |
| Type safety | Runtime errors | Compile-time |
| Method finding | String-based | DSL queries |
| Error messages | Generic | Detailed |
| Debugging | Manual logging | Built-in tools |

---

## Project Setup

### 1. Gradle Configuration

#### settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://api.xposed.info/")  // Required for Xposed API
    }
}
```

#### gradle/libs.versions.toml
```toml
[versions]
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"           # Must match Kotlin version
yukihookapi = "1.3.1"         # Latest stable (Dec 2024)
kavaref = "1.0.2"             # Required for YukiHookAPI 1.3.x
hiddenapibypass = "6.1"       # Replaces FreeReflection

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[libraries]
# YukiHookAPI
yukihookapi-api = { module = "com.highcapable.yukihookapi:api", version.ref = "yukihookapi" }
yukihookapi-ksp-xposed = { module = "com.highcapable.yukihookapi:ksp-xposed", version.ref = "yukihookapi" }

# KavaRef - Reflection API (REQUIRED since v1.3.0)
kavaref-core = { module = "com.highcapable.kavaref:kavaref-core", version.ref = "kavaref" }
kavaref-extension = { module = "com.highcapable.kavaref:kavaref-extension", version.ref = "kavaref" }

# Hidden API Access (replaces FreeReflection since v1.3.0)
hiddenapibypass = { module = "org.lsposed.hiddenapibypass:hiddenapibypass", version.ref = "hiddenapibypass" }

# Xposed API
xposed-api = { module = "de.robv.android.xposed:api", version = "82" }
```

#### app/build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.module"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
        targetSdk = 35
    }
    
    buildFeatures {
        buildConfig = true  // REQUIRED for YukiHookAPI
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // YukiHookAPI
    implementation(libs.yukihookapi.api)
    ksp(libs.yukihookapi.ksp.xposed)
    
    // KavaRef reflection (REQUIRED for 1.3.x)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
    
    // Hidden API bypass (replaces FreeReflection)
    implementation(libs.hiddenapibypass)
    
    // Xposed API (compile-only)
    compileOnly(libs.xposed.api)
}
```

### 2. AndroidManifest.xml Configuration

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher">
        
        <!-- Xposed Module Metadata -->
        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        
        <meta-data
            android:name="xposeddescription"
            android:value="@string/xposed_description" />
        
        <meta-data
            android:name="xposedminversion"
            android:value="93" />
        
        <meta-data
            android:name="xposedsharedprefs"
            android:value="true" />
        
        <!-- Xposed Scope (for LSPosed) -->
        <meta-data
            android:name="xposedscope"
            android:resource="@array/xposed_scope" />
            
    </application>
</manifest>
```

### 3. Xposed Scope Configuration

#### res/values/arrays.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="xposed_scope">
        <item>android</item>
        <item>com.android.systemui</item>
        <item>com.google.android.gms</item>
        <!-- Add target packages -->
    </string-array>
</resources>
```

---

## Core Concepts

### Execution Flow

```
LSPosed Framework
    │
    ▼
_YukiHookXposedInit (Auto-generated)
    │
    ├─► onXposedEvent()  [Optional: Native Xposed events]
    │
    ├─► onInit()         [Configuration only]
    │       │
    │       └─► configs { }
    │
    └─► onHook()         [Hook logic]
            │
            └─► encase { }
                    │
                    ├─► loadZygote { }     [Zygote scope]
                    │
                    ├─► loadSystem { }     [system_server]
                    │
                    └─► loadApp { }        [Target apps]
```

### Scope Hierarchy

| Scope | Method | When Loaded | Use Case |
|-------|--------|-------------|----------|
| Zygote | `loadZygote { }` | Process fork | System-wide hooks |
| System | `loadSystem { }` | system_server | Framework hooks |
| App | `loadApp { }` | App start | App-specific hooks |

### Key Objects

| Object | Description | Available In |
|--------|-------------|--------------|
| `packageName` | Current package name | `loadApp { }` |
| `appClassLoader` | App's ClassLoader | `loadApp { }` |
| `appInfo` | ApplicationInfo | `loadApp { }` |
| `processName` | Current process | All scopes |
| `systemContext` | System context | `loadZygote { }` |

---

## Hook Entry Point

### Basic Structure

```kotlin
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    
    /**
     * Called once when module is loaded.
     * Use ONLY for configuration.
     */
    override fun onInit() = configs {
        // Debug settings
        debugLog {
            tag = "MyModule"
            isEnable = BuildConfig.DEBUG
        }
        isDebug = BuildConfig.DEBUG
    }
    
    /**
     * Called for each hook scope (Zygote, Apps).
     * All hook logic goes here.
     */
    override fun onHook() = encase {
        // Load hookers in order
        loadHooker(FirstHooker)
        loadHooker(SecondHooker)
        
        // Or use inline hooks
        loadApp(name = "com.target.app") {
            // Hook code here
        }
    }
    
    /**
     * Optional: Access native Xposed events.
     * Useful for compatibility with other libraries.
     */
    override fun onXposedEvent() {
        YukiXposedEvent.events {
            onInitZygote { sparam ->
                // StartupParam access
            }
            onHandleLoadPackage { lpparam ->
                // LoadPackageParam access
            }
        }
    }
}
```

### Annotation Parameters

```kotlin
@InjectYukiHookWithXposed(
    // Custom source path (if not src/main)
    sourcePath = "src/main",
    
    // Explicit module package name
    modulePackageName = "com.example.module",
    
    // Custom Xposed entry class name
    entryClassName = "HookXposedEntry",
    
    // Enable module status API
    isUsingXposedModuleStatus = true,
    
    // Enable Resources hook support
    isUsingResourcesHook = false
)
object HookEntry : IYukiHookXposedInit { }
```

---

## Method Hooking

### Finding Methods

#### By Name
```kotlin
"com.example.Class".toClass().method {
    name = "methodName"
}.hook { }
```

#### By Parameters
```kotlin
method {
    name = "methodName"
    param(StringClass, IntType, BooleanType)
}

// Or using KClass
method {
    name = "methodName"
    param(String::class, Int::class, Boolean::class)
}
```

#### By Parameter Count
```kotlin
method {
    name = "overloadedMethod"
    paramCount = 2
}
```

#### By Return Type
```kotlin
method {
    name = "getResult"
    returnType = StringClass
}
```

#### By Modifiers
```kotlin
method {
    name = "privateMethod"
    modifiers { isPrivate && !isStatic }
}
```

#### First/Last Matching
```kotlin
// First method starting with "get"
firstMethod {
    name { it.startsWith("get") }
}

// Last method with no parameters
lastMethod {
    emptyParam()
}
```

#### All Matching Methods
```kotlin
// Hook all overloads
allMethods {
    name = "overloadedMethod"
}.forEach { method ->
    method.hook { }
}
```

### Hook Callbacks

```kotlin
method { name = "targetMethod" }.hook {
    
    // Before method execution
    before {
        // Read arguments
        val arg0 = args[0] as String
        val arg1 = args[1] as Int
        
        // Modify arguments
        args[0] = "modified"
        
        // Get instance (for instance methods)
        val obj = instance<TargetClass>()
        
        // Skip original method and set return value
        result = "earlyReturn"
        
        // Throw exception instead of executing
        throwable = IllegalStateException("Blocked")
    }
    
    // After method execution
    after {
        // Read original result
        val original = result as String
        
        // Modify result
        result = "newValue"
        
        // Access instance fields
        val field = instance<TargetClass>().current()
            .field { name = "mField" }
            .string()
    }
}
```

### Replace Methods

```kotlin
// Replace with custom implementation (no return)
method { name = "voidMethod" }.hook {
    replaceUnit {
        // Custom implementation
        instance<TargetClass>().customLogic()
    }
}

// Replace with return value
method { name = "getValue" }.hook {
    replaceAny {
        "customValue"
    }
}

// Replace with specific value
method { name = "isEnabled" }.hook {
    replaceToTrue()  // Always return true
}

method { name = "getObject" }.hook {
    replaceToNull()  // Always return null
}

method { name = "getCount" }.hook {
    replaceTo(42)    // Always return 42
}
```

### Constructor Hooking

```kotlin
"com.example.MyClass".toClass().apply {
    
    // Default constructor
    constructor().hook {
        after {
            instance<Any>().current()
                .field { name = "initialized" }
                .set(true)
        }
    }
    
    // Parameterized constructor
    constructor {
        param(StringClass, IntType)
    }.hook {
        before {
            args[0] = "intercepted"
        }
    }
    
    // All constructors
    allConstructors().forEach { it.hook { } }
}
```

---

## Field Access

### Static Fields

```kotlin
// Read static field
val model = "android.os.Build".toClass()
    .field { name = "MODEL" }
    .get()
    .string()

// Write static field
"android.os.Build".toClass()
    .field { name = "MODEL" }
    .get()
    .set("CustomModel")

// Find by type
"com.example.Class".toClass()
    .field { type = StringClass }
    .get()
    .string()
```

### Instance Fields

```kotlin
// Within hook callback
method { name = "someMethod" }.hook {
    after {
        // Read instance field
        val value = instance<TargetClass>().current()
            .field { name = "mValue" }
            .int()
        
        // Write instance field
        instance<TargetClass>().current()
            .field { name = "mValue" }
            .set(newValue)
    }
}
```

### Field Modifiers

```kotlin
field {
    name = "CONSTANT"
    modifiers { isStatic && isFinal }
}
```

---

## Class Loading

### Basic Loading

```kotlin
// Standard loading (throws if not found)
val clazz = "com.example.MyClass".toClass()

// Safe loading (returns null if not found)
val maybeClass = "com.example.MaybeClass".toClassOrNull()

// With specific ClassLoader
val clazz = "com.example.MyClass".toClass(appClassLoader)

// Check existence
if ("com.example.MyClass".hasClass()) {
    // Class exists
}
```

### Lazy Loading

```kotlin
// Defer loading until first use
val lazyClass = lazyClass("com.example.HeavyClass")

// Use later
lazyClass.method { name = "doSomething" }.hook { }
```

### Class Search

```kotlin
// Find classes matching criteria
searchClass {
    // By name patterns
    fullName {
        contains("Manager")
        endsWith("Impl")
    }
    
    // By methods
    methods {
        name = "getInstance"
        modifiers { isStatic }
    }
    
    // By fields
    fields {
        type = StringClass
    }
}.forEach { foundClass ->
    foundClass.method { name = "init" }.hook { }
}
```

### Superclass Access

```kotlin
// Access superclass methods
instance<ChildClass>().current().superClass()?.method {
    name = "parentMethod"
}?.call()

// Or specify in finder
method {
    name = "inheritedMethod"
    superClass()  // Include superclass search
}
```

---

## Error Handling

### Non-Blocking Errors

These log errors but don't crash:

```kotlin
// Class not found - will log error, continue
"com.maybe.NotExist".toClass().method { name = "test" }.hook { }

// Use ignoredHookClassNotFoundFailure to suppress
"com.maybe.NotExist".toClass()
    .ignoredHookClassNotFoundFailure()
    .method { name = "test" }
    .hook { }
```

### Optional Methods

```kotlin
// Method might not exist in all versions
method { name = "newApiMethod" }.optional().hook {
    // Only hooked if method exists
}
```

### Safe Execution

```kotlin
loadApp(name = "com.target.app") {
    runCatching {
        "com.example.Class".toClass().method { }.hook { }
    }.onFailure { error ->
        YLog.error("Hook failed: ${error.message}")
    }
}
```

### Exception Types

| Exception | Cause | Solution |
|-----------|-------|----------|
| `NoClassDefFoundError` | Class not found | Check package name, ClassLoader |
| `NoMethodException` | Method not found | Verify name, params, return type |
| `NoFieldException` | Field not found | Check name, type |
| `IllegalStateException` | Wrong usage | Follow API conventions |

---

## Best Practices

### 1. Module Architecture

```
com.example.module/
├── hook/
│   ├── HookEntry.kt              # Entry point
│   └── hooker/
│       ├── BaseHooker.kt         # Shared utilities
│       ├── SystemHooker.kt       # System hooks
│       └── AppHooker.kt          # App-specific hooks
├── data/
│   ├── repository/               # Data access
│   └── models/                   # Data classes
├── ui/                           # Module app UI
└── utils/                        # Utilities
```

### 2. Hooker Organization

```kotlin
// Use object for singleton hookers
object DeviceHooker : YukiBaseHooker() {
    
    override fun onHook() {
        // Group related hooks
        hookTelephony()
        hookBuild()
        hookSettings()
    }
    
    private fun hookTelephony() {
        "android.telephony.TelephonyManager".toClass().apply {
            // All TelephonyManager hooks
        }
    }
    
    private fun hookBuild() {
        "android.os.Build".toClass().apply {
            // All Build hooks
        }
    }
}
```

### 3. Performance

```kotlin
// Cache class references
private val telephonyClass by lazy {
    "android.telephony.TelephonyManager".toClass()
}

// Use inline hooks for simple cases
method { name = "getValue" }.hook {
    replaceAny { cachedValue }  // Fast path
}

// Avoid expensive operations in before/after
before {
    // DON'T: Complex computations here
    // DO: Pre-compute values outside hooks
}
```

### 4. Compatibility

```kotlin
// Check API level before hooking
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    method { name = "newMethod" }.hook { }
}

// Use optional for uncertain methods
method { name = "maybeExists" }.optional().hook { }

// Handle multiple versions
val methodName = if (Build.VERSION.SDK_INT >= 30) "newName" else "oldName"
method { name = methodName }.hook { }
```

### 5. Debugging

```kotlin
// Enable debug mode in development
override fun onInit() = configs {
    isDebug = BuildConfig.DEBUG
    debugLog {
        tag = "MyModule"
        isEnable = BuildConfig.DEBUG
    }
}

// Use YLog for consistent logging
before {
    YLog.debug("Method called with args: ${args.toList()}")
}

after {
    YLog.debug("Method returned: $result")
}
```

---

## Common Mistakes

### ❌ Starting hooks without scope

```kotlin
// WRONG
override fun onHook() = encase {
    "com.example.Class".toClass().method { }.hook { }
}

// CORRECT
override fun onHook() = encase {
    loadApp(name = "com.target.app") {
        "com.example.Class".toClass().method { }.hook { }
    }
}
```

### ❌ Calling encase in onInit

```kotlin
// WRONG
override fun onInit() {
    encase { }  // Exception!
}

// CORRECT
override fun onInit() = configs { }
override fun onHook() = encase { }
```

### ❌ Using class instead of object

```kotlin
// WRONG - Multiple instances in multi-package scenarios
class MyHooker : YukiBaseHooker() { }

// CORRECT - Singleton
object MyHooker : YukiBaseHooker() { }
```

### ❌ Forgetting to check packageName

```kotlin
// WRONG - Hooks every app
loadApp {
    "com.example.Class".toClass().hook { }
}

// CORRECT - Specific app
loadApp(name = "com.target.app") {
    "com.example.Class".toClass().hook { }
}
```

### ❌ Not handling optional methods

```kotlin
// WRONG - Crashes if method doesn't exist
method { name = "newApiMethod" }.hook { }

// CORRECT - Graceful handling
method { name = "newApiMethod" }.optional().hook { }
```

---

## Migration from Xposed API

### Class Loading

```kotlin
// Old: XposedHelpers
val clazz = XposedHelpers.findClass("com.example.Class", lpparam.classLoader)

// New: YukiHookAPI
val clazz = "com.example.Class".toClass()
```

### Method Hooking

```kotlin
// Old: XposedHelpers
XposedHelpers.findAndHookMethod(
    "com.example.Class",
    lpparam.classLoader,
    "methodName",
    String::class.java,
    object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            param.args[0] = "modified"
        }
        override fun afterHookedMethod(param: MethodHookParam) {
            param.result = "newResult"
        }
    }
)

// New: YukiHookAPI
"com.example.Class".toClass()
    .method {
        name = "methodName"
        param(StringClass)
    }
    .hook {
        before { args[0] = "modified" }
        after { result = "newResult" }
    }
```

### Field Access

```kotlin
// Old: XposedHelpers
val value = XposedHelpers.getStaticObjectField(clazz, "FIELD_NAME") as String
XposedHelpers.setStaticObjectField(clazz, "FIELD_NAME", "newValue")

// New: YukiHookAPI
val value = clazz.field { name = "FIELD_NAME" }.get().string()
clazz.field { name = "FIELD_NAME" }.get().set("newValue")
```

### Instance Method Call

```kotlin
// Old: XposedHelpers
XposedHelpers.callMethod(instance, "methodName", arg1, arg2)

// New: YukiHookAPI (in hook context)
instance<Any>().current().method {
    name = "methodName"
}.call(arg1, arg2)
```

---

## YukiHookAPI 1.3.x Migration Notes

### Breaking Changes in 1.3.0+

1. **YukiHookAPI Reflection API Deprecated** → Use KavaRef
   - YukiHookAPI's built-in reflection is deprecated
   - Migrate to `com.highcapable.kavaref:kavaref-core` and `kavaref-extension`

2. **ModuleAppActivity/ModuleAppCompatActivity Deprecated**
   ```kotlin
   // OLD (Deprecated)
   class SettingsActivity : ModuleAppCompatActivity()
   
   // NEW
   class SettingsActivity : ModuleActivity()
   ```

3. **FreeReflection Replaced by AndroidHiddenApiBypass**
   - Now uses `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`
   - Better compatibility with newer Android versions

4. **Duplicate Hook Limitation Removed**
   - You can now hook the same method multiple times
   - Useful for modular hooking strategies

5. **YLog Enhanced**
   - `msg` parameter now accepts any object
   - Auto-converts to string for printing
   ```kotlin
   YLog.debug(dataObject)  // Works in 1.3.x
   ```

### 1.3.1 Bug Fixes
- Fixed Activity proxy issues on Android 9 and below
- Fixed return value checking when Hook method returns `Object`
- Updated dependencies for BetterAndroid compatibility

---

## Resources

### Official Documentation
- [YukiHookAPI Docs](https://highcapable.github.io/YukiHookAPI/en/)
- [KavaRef Docs](https://highcapable.github.io/KavaRef/en/)
- [GitHub Repository](https://github.com/HighCapable/YukiHookAPI)
- [Changelog](https://highcapable.github.io/YukiHookAPI/en/about/changelog)

### Related Projects
- [LSPosed](https://github.com/LSPosed/LSPosed) - Xposed Framework
- [LSPatch](https://github.com/LSPosed/LSPatch) - Root-free Xposed
- [AndroidHiddenApiBypass](https://github.com/LSPosed/AndroidHiddenApiBypass) - Hidden API Access

---

**Document Version:** 2.0  
**YukiHookAPI Version:** 1.3.1  
**KavaRef Version:** 1.0.2  
**Last Updated:** December 15, 2025
