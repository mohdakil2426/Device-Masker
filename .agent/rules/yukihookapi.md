---
trigger: always_on
---

# PrivacyShield - YukiHookAPI Development Rules
# LSPosed/Xposed Module with YukiHookAPI 1.3.1

## Tech Stack
- **YukiHookAPI** 1.3.1 - High-level Kotlin Hook API framework (Latest: Dec 2024)
- **KavaRef** 1.0.2 - Core reflection API (REQUIRED since v1.3.0)
- **Kotlin** 2.2.21 | **KSP** 2.2.21-2.0.4
- **AndroidHiddenApiBypass** 6.1 - Hidden API access (replaces FreeReflection)
- **Material 3 Expressive** + Jetpack Compose + DataStore

---

## Hook Entry Point Structure

```kotlin
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    
    // Configure API settings in onInit() - NOT hooking logic
    override fun onInit() = configs {
        debugLog { 
            tag = "PrivacyShield"
            isEnable = BuildConfig.DEBUG 
        }
        isDebug = BuildConfig.DEBUG
    }
    
    // All hook logic goes in onHook()
    override fun onHook() = encase {
        loadHooker(AntiDetectHooker)  // Anti-detection FIRST!
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
    }
}
```

---

## ❌ NEVER DO These Things

```kotlin
// ❌ NEVER use raw XposedHelpers - use YukiHookAPI DSL
XposedHelpers.findAndHookMethod("Class", loader, "method", callback)

// ✅ CORRECT: Use YukiHookAPI
"com.example.Class".toClass().method { name = "methodName" }.hook { }

// ❌ NEVER call encase() in onInit() or onXposedEvent()
override fun onInit() {
    YukiHookAPI.encase { } // Will throw exception!
}

// ❌ NEVER start hooks directly without scope
override fun onHook() = encase {
    "com.example.Class".toClass().method { }.hook { }  // Wrong!
}

// ✅ CORRECT: Use loadApp() or loadZygote() first
override fun onHook() = encase {
    loadApp(name = "com.target.app") {
        "com.example.Class".toClass().method { }.hook { }
    }
}

// ❌ NEVER use class for Hookers (use object for singleton)
class DeviceHooker : YukiBaseHooker() { }  // Wrong!

// ✅ CORRECT
object DeviceHooker : YukiBaseHooker() { }
```

---

## YukiBaseHooker Pattern

```kotlin
object DeviceHooker : YukiBaseHooker() {
    
    override fun onHook() {
        // Always check app scope first
        loadApp(name = "com.target.app") {
            
            // Hook methods using DSL
            "android.telephony.TelephonyManager".toClass().apply {
                
                // Basic method hook
                method { 
                    name = "getDeviceId" 
                }.hook {
                    after { result = spoofedValue }
                }
                
                // Handle overloads with paramCount
                method { 
                    name = "getImei"
                    paramCount = 0 
                }.hook {
                    after { result = spoofedIMEI }
                }
                
                // Use optional() for methods that might not exist
                method { 
                    name = "getMeid" 
                }.optional().hook {
                    after { result = spoofedMEID }
                }
            }
        }
    }
}
```

---

## Method Finding Best Practices

```kotlin
// By name only
method { name = "methodName" }

// By name and parameters
method { 
    name = "methodName"
    param(StringClass, IntType, BooleanType)
}

// By parameter count (for overloads)
method {
    name = "methodName"
    paramCount = 2
}

// By return type
method {
    name = "methodName"
    returnType = StringClass
}

// By modifiers
method {
    name = "privateMethod"
    modifiers { isPrivate && !isStatic }
}

// First/last method matching criteria
firstMethod { name { it.startsWith("get") } }
lastMethod { emptyParam() }

// All methods matching criteria
allMethods { name = "overloadedMethod" }
```

---

## Hook Callbacks

```kotlin
method { name = "targetMethod" }.hook {
    
    // Before method execution
    before {
        val arg0 = args[0] as String      // Access arguments
        args[0] = "modified"               // Modify arguments
        result = "returnValue"             // Skip method execution
        throwable = Exception("Blocked")   // Throw exception
        val obj = instance<TargetClass>()  // Access instance
    }
    
    // After method execution
    after {
        result = "newValue"                // Modify return value
        val original = result as String    // Access original result
        instance<TargetClass>().field = x  // Access instance
    }
    
    // Replace entirely
    replaceUnit { customImplementation() }
    replaceAny { "customReturn" }
    replaceToTrue()
    replaceTo(42)
    replaceToNull()
}
```

---

## Class Loading & Field Access

```kotlin
// Basic class loading
val clazz = "com.example.MyClass".toClass()

// Safe loading (returns null if not found)
val maybeClass = "com.example.MaybeClass".toClassOrNull()

// With specific ClassLoader
val clazz = "com.example.MyClass".toClass(appClassLoader)

// Check if class exists
if ("com.example.MyClass".hasClass()) { }

// Lazy class loading (deferred until first use)
val lazyClass = lazyClass("com.example.HeavyClass")

// Search for classes in DEX
searchClass {
    fullName { contains("Manager"); endsWith("Impl") }
    methods { name = "initialize"; modifiers { isStatic } }
}.forEach { it.method { name = "init" }.hook { } }
```

### Field Access

```kotlin
// Get/Set static field
val value = "android.os.Build".toClass().field { name = "MODEL" }.get().string()
"android.os.Build".toClass().field { name = "MODEL" }.get().set("SpoofedModel")

// Get/Set instance field (in hook callback)
instance<TargetClass>().current().field { name = "mField" }.string()
instance<TargetClass>().current().field { name = "mField" }.set("newValue")
```

### Constructor Hooking

```kotlin
"com.example.MyClass".toClass().apply {
    constructor().hook {
        after { instance<Any>().current().field { name = "init" }.set(true) }
    }
    constructor { param(StringClass, IntType) }.hook {
        before { args[0] = "modified" }
    }
}
```

---

## Anti-Detection Requirements (CRITICAL)

### 1. Load Order - Anti-detection MUST be FIRST
```kotlin
override fun onHook() = encase {
    loadHooker(AntiDetectHooker)  // ⚠️ MUST BE FIRST
    loadHooker(DeviceHooker)      // Then spoofing hooks
    loadHooker(NetworkHooker)
}
```

### 2. Hidden Patterns (Always Include)
```kotlin
private val HIDDEN_PATTERNS = listOf(
    "de.robv.android.xposed",
    "io.github.lsposed",
    "com.highcapable.yukihookapi",
    "EdHooker", "LSPHooker", "XposedBridge",
    "XC_MethodHook", "XposedHelpers"
)
```

### 3. Stack Trace Filtering
```kotlin
// Filter Thread.getStackTrace()
"java.lang.Thread".toClass().method { name = "getStackTrace" }.hook {
    after {
        val stack = result as? Array<StackTraceElement> ?: return@after
        result = stack.filterNot { element ->
            HIDDEN_PATTERNS.any { element.className.contains(it) }
        }.toTypedArray()
    }
}

// Filter Throwable.getStackTrace() - same pattern
"java.lang.Throwable".toClass().method { name = "getStackTrace" }.hook { ... }

// Block Class.forName() for Xposed classes
"java.lang.Class".toClass().method { name = "forName"; paramCount = 1 }.hook {
    before {
        val className = args[0] as? String ?: return@before
        if (HIDDEN_PATTERNS.any { className.contains(it) }) {
            throwable = ClassNotFoundException(className)
        }
    }
}
```

---

## Value Generation Standards

| Type | Format | Rules |
|------|--------|-------|
| **IMEI** | 15 digits | Luhn-valid, TAC prefixes (35, 86, 01, 45) |
| **MAC** | XX:XX:XX:XX:XX:XX | 6 octets, unicast (LSB of 1st=0) |
| **Serial** | Alphanumeric | 8-16 characters |
| **Fingerprint** | brand/device/device:SDK/BUILD_ID:type/tags | Realistic format |

---

## Gradle Configuration

### libs.versions.toml
```toml
[versions]
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"
yukihookapi = "1.3.1"
kavaref = "1.0.2"
hiddenapibypass = "6.1"

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[libraries]
yukihookapi-api = { module = "com.highcapable.yukihookapi:api", version.ref = "yukihookapi" }
yukihookapi-ksp-xposed = { module = "com.highcapable.yukihookapi:ksp-xposed", version.ref = "yukihookapi" }
kavaref-core = { module = "com.highcapable.kavaref:kavaref-core", version.ref = "kavaref" }
kavaref-extension = { module = "com.highcapable.kavaref:kavaref-extension", version.ref = "kavaref" }
hiddenapibypass = { module = "org.lsposed.hiddenapibypass:hiddenapibypass", version.ref = "hiddenapibypass" }
xposed-api = { module = "de.robv.android.xposed:api", version = "82" }
```

### app/build.gradle.kts
```kotlin
plugins { alias(libs.plugins.ksp) }

android { 
    buildFeatures { buildConfig = true }  // REQUIRED for YukiHookAPI
}

dependencies {
    implementation(libs.yukihookapi.api)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
    implementation(libs.hiddenapibypass)
    compileOnly(libs.xposed.api)
    ksp(libs.yukihookapi.ksp.xposed)
}
```

### AndroidManifest.xml
```xml
<meta-data android:name="xposedmodule" android:value="true" />
<meta-data android:name="xposeddescription" android:value="Module description" />
<meta-data android:name="xposedminversion" android:value="93" />
<meta-data android:name="xposedsharedprefs" android:value="true" />
```

---

## Error Handling

```kotlin
// Use ignoredHookClassNotFoundFailure for optional classes
"com.optional.Class".toClass()
    .ignoredHookClassNotFoundFailure()
    .method { name = "optionalMethod" }
    .hook { }

// Use optional() for uncertain methods
method { name = "maybeExistsMethod" }.optional().hook { }

// Wrap hook logic in try-catch for stability
loadApp(name = "com.target.app") {
    runCatching {
        "com.example.Class".toClass().method { }.hook { }
    }.onFailure { YLog.error(it) }
}
```

## Logging
```kotlin
YLog.debug("Debug message")
YLog.info("Info message")
YLog.warn("Warning message")
YLog.error("Error message", exception)
```

---

## Package Structure
```
com.akil.privacyshield/
├── hook/
│   ├── HookEntry.kt              # @InjectYukiHookWithXposed entry
│   └── hooker/
│       ├── AntiDetectHooker.kt   # Anti-detection (loads first)
│       ├── DeviceHooker.kt       # Device identifier spoofing
│       └── NetworkHooker.kt      # Network identifier spoofing
├── data/
│   ├── repository/, datastore/, models/
│   └── generators/{IMEI,MAC,Fingerprint}Generator.kt
├── ui/MainActivity.kt, screens/, theme/
└── utils/Constants.kt
```

## File Naming: `*Hooker.kt`, `*Generator.kt`, `*Screen.kt`, `*Repository.kt`

---

## Quick Reference

### Execution Flow
```
_YukiHookXposedInit → onXposedEvent() → onInit() [configs only] → onHook() [hook logic]
```

### Scope Methods
- `loadZygote { }` - Hook in Zygote (system-wide)
- `loadApp(name = "pkg") { }` - Hook specific app
- `loadSystem { }` - Hook system_server

### HookParam Modifiers
- `result = value` - Set return value
- `args[0] = value` - Modify argument
- `throwable = Exception()` - Throw exception
- `instance<T>()` - Get hooked instance

---

## YukiHookAPI 1.3.x Migration Notes

### Breaking Changes in 1.3.0+

1. **Reflection API Deprecated** → Use KavaRef
   - Migrate to `kavaref-core` and `kavaref-extension`

2. **ModuleAppActivity/ModuleAppCompatActivity Deprecated**
   ```kotlin
   // OLD: class SettingsActivity : ModuleAppCompatActivity()
   // NEW: class SettingsActivity : ModuleActivity()
   ```

3. **FreeReflection Replaced** → Use `AndroidHiddenApiBypass`
   - Add: `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`

4. **Duplicate Hook Limitation Removed**
   - Can now hook same method multiple times

5. **YLog Enhanced** - `msg` accepts any object, auto-converts to string

### 1.3.1 Bug Fixes
- Fixed Activity proxy on Android 9 and below
- Fixed return value check when Hook method returns `Object`
- Updated dependencies for BetterAndroid compatibility

---

**Version:** 2.0 | **Framework:** YukiHookAPI 1.3.1 | **KavaRef:** 1.0.2 | **Updated:** Dec 15, 2025
