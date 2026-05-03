# Project Setup Reference — libxposed Modern Xposed API

**Official source:** https://github.com/libxposed/api  
**Example module:** https://github.com/libxposed/example  
**Maven Central:** https://central.sonatype.com/artifact/io.github.libxposed/api

---

## 1. Maven Central — Dependency Coordinates

All artifacts are published to Maven Central under group `io.github.libxposed`.

### Maven XML

```xml
<!-- Core API — module developers use scope=provided/compileOnly -->
<dependency>
    <groupId>io.github.libxposed</groupId>
    <artifactId>api</artifactId>
    <version>101.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- Service library — for module app UI IPC -->
<dependency>
    <groupId>io.github.libxposed</groupId>
    <artifactId>service</artifactId>
    <version><!-- check https://mvnrepository.com/artifact/io.github.libxposed/service --></version>
</dependency>
```

### Gradle Kotlin DSL (`build.gradle.kts`)

```kotlin
android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26          // API 101 requires minSdk 26 (Android 8.0)
        targetSdk = 35
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // Core API — MUST be compileOnly for module developers.
    // The framework provides the real implementation at runtime.
    // Using 'implementation' would embed dead stubs into your APK.
    compileOnly("io.github.libxposed:api:101.0.1")

    // Service library — for module app UI ↔ framework IPC
    // Check latest: https://central.sonatype.com/artifact/io.github.libxposed/service
    implementation("io.github.libxposed:service:<latest>")

    // Helper library — replaces XposedHelpers reflection utilities
    // Check latest: https://central.sonatype.com/artifact/io.github.libxposed/helper
    implementation("io.github.libxposed:helper:<latest>")

    // Kotlin extensions for helper
    implementation("io.github.libxposed:helper-ktx:<latest>")
}
```

### Gradle Groovy DSL (`build.gradle`)

```groovy
dependencies {
    compileOnly 'io.github.libxposed:api:101.0.1'
    implementation 'io.github.libxposed:service:<latest>'
}
```

### Version Checking

Always check latest at:
- https://central.sonatype.com/artifact/io.github.libxposed/api (versions tab)
- https://mvnrepository.com/artifact/io.github.libxposed/api
- https://mvnrepository.com/artifact/io.github.libxposed/service
- https://mvnrepository.com/artifact/io.github.libxposed/helper

The artifact is a standard Android AAR. The API also depends on `androidx.annotation:annotation` which will be pulled transitively.

---

## 2. ProGuard / R8 Rules

The API library ships its own consumer ProGuard rules via `consumerProguardFiles`. These are applied automatically — you do **not** need to copy them manually. They prevent obfuscation of critical API classes.

However, you must add this rule to support renaming your `java_init.list` entry class:

```proguard
# Allow R8 to rename resource file paths to match renamed classes.
# This is needed so META-INF/xposed/java_init.list stays in sync after obfuscation.
-adaptresourcefilenames META-INF/xposed/java_init.list

# Keep your own entry class (adapt to actual class name)
-keep class com.example.mymodule.MyModule { <init>(); }
```

---

## 3. META-INF Files

Location: `src/main/resources/META-INF/xposed/`

Gradle automatically packages these into the APK. No additional configuration needed.

### `java_init.list`

```
com.example.mymodule.MyModule
```

One fully-qualified class name per line. The class must extend `XposedModule`.  
Multiple entry classes are supported — add one per line.

Native entries use `native_init.list` instead (`.so` name per line).

### `module.prop`

Java `Properties` format. Key-value pairs:

```properties
# Required
minApiVersion=101
targetApiVersion=101

# Optional: lock scope so users cannot add apps outside scope.list
staticScope=false

# Optional: global exception mode (protective | passthrough)
# Defaults to protective if omitted.
exceptionMode=protective
```

| Key | Type | Required | Description |
|---|---|---|---|
| `minApiVersion` | int | Yes | Minimum Xposed API version required |
| `targetApiVersion` | int | Yes | Target Xposed API version |
| `staticScope` | boolean | No | If true, users cannot apply module on apps outside scope.list |
| `exceptionMode` | string | No | `protective` (default) or `passthrough` |

### `scope.list`

```
com.example.targetapp
com.android.settings
system
```

One package name per line. Special values:
- `system` → targets system server process (not a real package name)
- `android` → targets the android framework `:ui` process (valid; some components aren't in system server)

**Important:** Do NOT add packages whose all components run in system server (e.g., `com.android.providers.settings`). Use `system` for those.

---

## 4. Android Manifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.mymodule">

    <application
        android:label="@string/module_name"
        android:description="@string/module_description"
        android:icon="@mipmap/ic_launcher">

        <!-- Required: declare this as an Xposed module -->
        <meta-data
            android:name="xposedmodule"
            android:value="true" />

        <!-- Required: minimum Xposed API version -->
        <meta-data
            android:name="xposedminversion"
            android:value="101" />

        <!-- Required ONLY if using the service library (io.github.libxposed:service) -->
        <!-- This ContentProvider is how the framework connects to the module app -->
        <provider
            android:name="io.github.libxposed.service.XposedProvider"
            android:authorities="${applicationId}.xposed"
            android:exported="true" />

    </application>
</manifest>
```

`android:label` = module name shown in Xposed manager  
`android:description` = description shown in Xposed manager  
These **replace** the old `xposed_description` meta-data from legacy API.

---

## 5. Full File Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/mymodule/
│   │   │   ├── MyModule.java               ← extends XposedModule
│   │   │   └── hooks/
│   │   │       └── MyHooker.java           ← implements XposedInterface.Hooker
│   │   ├── res/
│   │   │   └── values/strings.xml          ← module_name, module_description
│   │   ├── resources/
│   │   │   └── META-INF/xposed/
│   │   │       ├── java_init.list          ← entry class FQN
│   │   │       ├── module.prop             ← minApiVersion, targetApiVersion
│   │   │       └── scope.list             ← target packages
│   │   └── AndroidManifest.xml
├── build.gradle.kts
└── proguard-rules.pro
```

---

## 6. Lifecycle Callback Order

```
XposedModule() ← no-arg constructor; DO NOT init anything here
    │
    ▼
attachFramework(XposedInterface)  ← automatic; NEVER call manually
    │
    ▼
onModuleLoaded(ModuleLoadedParam)
    │  Safe to use: hook(), getInvoker(), log(), getFrameworkProperties(), etc.
    │  param.getProcessName()  — current process name
    │  param.isSystemServer()  — true if in system server
    ▼
onSystemServerStarting(SystemServerStartingParam)  ← system scope only
    │  param.getClassLoader()  — system server class loader
    ▼
onPackageLoaded(PackageLoadedParam)  ← @RequiresApi(29)
    │  Default classloader ready; before AppComponentFactory instantiation
    │  param.getPackageName()
    │  param.getApplicationInfo()
    │  param.isFirstPackage()         — true if main/first package in this process
    │  param.getDefaultClassLoader()  — @RequiresApi(29)
    ▼
onPackageReady(PackageReadyParam)   ← extends PackageLoadedParam
    │  Full app classloader ready — best place for most hooks
    │  param.getClassLoader()         — may differ from getDefaultClassLoader()
    │                                   if custom AppComponentFactory is in use
    │  param.getAppComponentFactory() — @RequiresApi(28)
    │  + all methods from PackageLoadedParam
    ▼
(hooks fire as methods are called in the app)
```

**Note:** `onPackageLoaded` and `onPackageReady` can both be called **multiple times** for the same process — once per package loaded. Always filter by `param.getPackageName()`.

---

## 7. API Version Check at Runtime

```java
@Override
public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
    int runtimeApi = getApiVersion();       // runtime version
    int libApi = XposedInterface.LIB_API;   // compile-time version (static)

    long props = getFrameworkProperties();
    boolean canHookSystem = (props & PROP_CAP_SYSTEM) != 0;
    boolean hasRemote     = (props & PROP_CAP_REMOTE) != 0;
    boolean apiProtected  = (props & PROP_RT_API_PROTECTION) != 0;
    // PROP_RT_ prefixed properties may change between launches
}
```

`PROP_RT_API_PROTECTION` — framework disallows accessing Xposed API via reflection or dynamically loaded code.
