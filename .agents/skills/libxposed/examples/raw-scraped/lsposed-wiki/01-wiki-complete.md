# RAW SCRAPE — LSPosed Official Wiki
# Source: https://github.com/LSPosed/LSPosed/wiki
# Scraped: 2025-05-03
# Note: LSPosed repo was archived on Mar 27, 2026 (read-only)

---

# Page: Home
# https://github.com/LSPosed/LSPosed/wiki

## LSPosed official wiki

基于 Riru/Zygisk 的 ART hook 框架，提供与原版 Xposed 相同的 API, 使用 LSPlant (https://github.com/LSPosed/LSPlant) 进行 hook。

> Xposed 框架是一套开放源代码的框架服务，可以在不修改APK文件的情况下修改目标应用的运行，基于它可以制作功能强大的模块，且在功能不冲突的情况下同时运作。

LSPosed 框架和原版 Xposed 框架的不同之处：
1. LSPosed 支持 Android 8.1 以后的版本
2. LSPosed 只有注入被勾选的应用，其他应用运行在干净的环境
3. 对于不需要注入系统服务的模块，重启目标应用即可激活
4. LSPosed 极难被检测，文件系统没有可疑痕迹，不需要安装独立的管理器应用

## Chat group / 用户讨论群组
- Telegram channel: @LSPosed (https://t.me/LSPosed)

## Wiki Pages
- Home: https://github.com/LSPosed/LSPosed/wiki
- Develop Xposed Modules Using Modern Xposed API: https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
- How to use it: https://github.com/LSPosed/LSPosed/wiki/How-to-use-it
- Module Scope: https://github.com/LSPosed/LSPosed/wiki/Module-Scope
- Native Hook: https://github.com/LSPosed/LSPosed/wiki/Native-Hook
- New XSharedPreferences: https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
- 如何使用: https://github.com/LSPosed/LSPosed/wiki/%E5%A6%82%E4%BD%95%E4%BD%BF%E7%94%A8

---

# Page: Develop Xposed Modules Using Modern Xposed API
# https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
# Last edited: Mar 17, 2026 (19 revisions) — Nullptr

This is a brief document about developing Xposed Modules using modern Xposed API.

## API Changes

Compared to the legacy XposedBridge APIs, the modern API has the following differences:

1. Java entry now uses `META-INF/xposed/java_init.list` instead of `assets/xposed_init`; native
   entry now uses `META-INF/xposed/native_init.list`. Create a file in `src/main/resources/META-INF`,
   and Gradle will automatically package files into your APK. You should now be able to obfuscate
   your entry classes with R8 with proguard rules `adaptresourcefilenames`.

2. Modern API does not use metadata anymore as well. Module name uses the `android:label` resource;
   module description uses the `android:description` resource; scope list uses
   `META-INF/xposed/scope.list` (one line for one package name); module configuration uses
   `META-INF/xposed/module.prop` (in format of Java properties).

3. Java entry should now implement `io.github.libxposed.api.XposedModule`. Note that `XposedModule`
   no longer receives `XposedInterface` and `ModuleLoadedParam` in its constructor; the framework
   calls `attachFramework(XposedInterface)` automatically. Modules **should not** perform
   initialization before `onModuleLoaded()` is called.

4. Hook APIs use an **OkHttp-style interceptor chain** model. Modules implement typed `Hooker<T>`
   interfaces with an `intercept(Chain<T> chain)` method. Hooking methods now return `HookBuilder`
   for configuring priority and exception mode. We no longer provide interfaces like `XposedHelpers`
   in the framework anymore. But we will offer official libraries for a more friendly development kit.
   See https://github.com/libxposed/helper for this developing library.

5. You can now deoptimize a specific method (accepting an `Executable` parameter) to bypass method
   inline (especially when hooking System Framework). We also introduce useful APIs through the
   **Invoker system**: use `getInvoker(Method)` or `getInvoker(Constructor)` to obtain an invoker,
   which provides `invokeSpecial` and `newInstanceSpecial` as methods on the invoker objects.

6. Resource hooks are removed. Resource hooks are hard to maintain and caused many problems
   previously, so it will not be supported.

7. You can communicate to the Xposed framework now. With the help of this feature, you can
   **dynamically request scope**, **share SharedPreferences or blob file** across your module and
   hooked app, **check framework's name and version**, and more... To achieve this, you should
   register an Xposed service listener in your module, and once your module app is launched, the
   Xposed framework will send you a service to communicate with the framework. See
   https://github.com/libxposed/service for more details. As a result, **module apps are no longer
   hooked by themselves**.

## Module Configuration Properties

| Name | Format | Optional | Meaning |
|---|---|---|---|
| minApiVersion | int | No | Indicates the minimal Xposed API version required by the module |
| targetApiVersion | int | No | Indicates the target Xposed API version required by the module |
| staticScope | boolean | Yes | Indicates whether users should not apply the module on any other app out of scope |

## Comparison Among Content Sharing APIs

| Name | API | Supported | Storage Location | Change Listener | Large Content |
|---|---|---|---|---|---|
| New XSharedPreferences | Legacy(ext) | ❌ Since v2.1.0 | /data/misc/<random>/prefs/<module> | ❌ | ❌ |
| XSharedPreferences | Legacy | ✅ Since v2.0.0 | Module apps' internal storage | ❌ | ❌ |
| Remote Preferences | Modern | ✅ Since v1.9.0 | LSPosed database | ✅ | ❌ |
| Remote Files | Modern | ✅ Since v1.9.0 | /data/adb/lspd/modules/<user>/<module> | ❌ | ✅ |

---

# Page: Module Scope
# https://github.com/LSPosed/LSPosed/wiki/Module-Scope
# Last edited: May 7, 2022 (1 revision) — vvb2060

## Background

Module scopes are required in LSPosed to activate a module. Of course, user can select their own
scope for a specific module, but it's too advanced for a user to determine the real module scope.
So in LSPosed, a new meta-data is introduced for module developers to declare its scope.

## Example usage

If a module wants to hook package `com.example.a` and `com.example.b`, in `AndroidManifest.xml`,
a meta-data named `xposedscope` should be defined as:

```xml
<meta-data
    android:name="xposedscope"
    android:resource="@array/example_scope" />
```

And in `array.xml`, a string array named `example_scope` should be defined as:

```xml
<string-array name="example_scope">
    <item>com.example.a</item>
    <item>com.example.b</item>
</string-array>
```

If no alternative resources, can hardcode it:

```xml
<meta-data
    android:name="xposedscope"
    android:value="com.example.a;com.example.b" />
```

## Advantage

By defining such a meta-data, all apps within this array will be selected by default when the
module is enabled in the manager.

---

# Page: Native Hook
# https://github.com/LSPosed/LSPosed/wiki/Native-Hook
# Last edited: May 7, 2022 (1 revision) — vvb2060

## How Native Hook Works

Native Hook can help you hook native functions of an app. Whenever a new native library is loaded,
LSPosed will invoke a callback of the module and the module can then perform hooks on this library.

## Header File

```c
typedef int (*HookFunType)(void *func, void *replace, void **backup);
typedef int (*UnhookFunType)(void *func);
typedef void (*NativeOnModuleLoaded)(const char *name, void *handle);

typedef struct {
    uint32_t version;
    HookFunType hook_func;
    UnhookFunType unhook_func;
} NativeAPIEntries;

typedef NativeOnModuleLoaded (*NativeInit)(const NativeAPIEntries *entries);
```

## Entries

```cpp
extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries);
```

The name `native_init` must be kept and exported.
`NativeAPIEntries` contains `hook_func` and `unhook_func`.
Do not modify its content — it will crash.
The return value is the callback function invoked when a library is loaded by `dlopen`.
You can perform hooks on system libraries in `native_init`.

## Callback

The callback function type: `NativeOnModuleLoaded`.
Called by LSPosed each time a library is loaded.
- `name` — the name of the loaded library (e.g. `/xxx/libtarget.so`)
- `handle` — the handle of the library for `dlsym`

## JNIEnv Hooks

To hook JNIEnv functions like `GetMethodId`: since the function is in `libart.so` which you cannot
easily `dlopen`, create `JNI_OnLoad` to receive a `JavaVm` from the Android system, then get a
`JNIEnv`. Then get the JNIEnv function pointers via the `JNIEnv` object.

## Tell Entries to Framework

Place library names in `assets/native_init` (one per line). Then manually call
`System.loadLibrary("yourlib")` when your module is loaded.

## Simple Example

assets/native_init:
```
libexample.so
```

assets/xposed_init:
```
org.lsposed.example.MainHook
```

AndroidManifest.xml:
```xml
<application
    android:multiArch="true"
    android:extractNativeLibs="false">
```

MainHook.kt:
```kotlin
package org.lsposed.example

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "org.lsposed.target") {
            try {
                System.loadLibrary("example")
            } catch (e: Throwable) {
                LogUtil.e(e)
            }
        }
    }
}
```

example.cc:
```cpp
static HookFunType hook_func = nullptr;

int (*backup)();
int fake() { return backup() + 1; }

FILE *(*backup_fopen)(const char *filename, const char *mode);
FILE *fake_fopen(const char *filename, const char *mode) {
    if (strstr(filename, "banned")) return nullptr;
    return backup_fopen(filename, mode);
}

jclass (*backup_FindClass)(JNIEnv *env, const char *name);
jclass fake_FindClass(JNIEnv *env, const char *name) {
    if (!strcmp(name, "dalvik/system/BaseDexClassLoader")) return nullptr;
    return backup_FindClass(env, name);
}

void on_library_loaded(const char *name, void *handle) {
    if (std::string(name).ends_with("libtarget.so")) {
        void *target = dlsym(handle, "target_fun");
        hook_func(target, (void *) fake, (void **) &backup);
    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void*) {
    JNIEnv *env = nullptr;
    jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    hook_func((void *)env->functions->FindClass, (void *)fake_FindClass, (void **)&backup_FindClass);
    return JNI_VERSION_1_6;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    hook_func((void*) fopen, (void*) fake_fopen, (void**) &backup_fopen);
    return on_library_loaded;
}
```

---

# Page: New XSharedPreferences
# https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences
# Last edited: Dec 2, 2022 (3 revisions) — Howard Wu
# NOTE: This is LEGACY API. Deprecated since v2.1.0. Use Remote Preferences (Modern API) instead.

## New XSharedPreferences

Since LSPosed API 93, a new `XSharedPreferences` is provided targeted for modules with sdk > 27.
Enable by setting `xposedminversion` to 93 or above, or adding `xposedsharedprefs` meta-data.

## For the module

LSPosed automatically hooks `ContextImpl.getPreferencesDir()` so that `Context.getSharedPreferences`
and `PreferenceFragment` work without further modification. Also hooks `ContextImpl.checkMode(int)`.

```java
// Java
SharedPreferences pref;
try {
    pref = context.getSharedPreferences(MY_PREF_NAME, Context.MODE_WORLD_READABLE);
} catch (SecurityException ignored) {
    pref = null; // fallback
}

// Kotlin
val pref = try {
    context.getSharedPreferences(MY_PREF_NAME, Context.MODE_WORLD_READABLE)
} catch (e: SecurityException) {
    null
}
```

## For the hooked app

```java
// Java (legacy API — for reading from hooked app)
private static XSharedPreferences getPref(String path) {
    XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, path);
    return pref.getFile().canRead() ? pref : null;
}
```

Cannot use `XSharedPreferences(File prefFile)` because the preference file is stored in a random directory.

## Preference change listener (hooked process)

```java
xsharedPrefs.registerOnSharedPreferenceChangeListener(listener);
// Note: preference key in callback will ALWAYS be null by design
// Call reload() after change to get fresh preferences
xsharedPrefs.unregisterOnSharedPreferenceChangeListener(listener);
```
