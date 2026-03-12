## ADDED Requirements

### Requirement: Hook ClassLoader.loadClass() for Xposed class hiding

The module SHALL hook `ClassLoader.loadClass(String)` to throw `ClassNotFoundException` when the requested class name matches known Xposed/LSPosed/YukiHookAPI class names.

#### Scenario: App tries to load XposedBridge via ClassLoader

- **WHEN** an app calls `getClass().getClassLoader().loadClass("de.robv.android.xposed.XposedBridge")`
- **THEN** the hook throws `ClassNotFoundException("de.robv.android.xposed.XposedBridge not found")`

#### Scenario: Legitimate class loading unaffected

- **WHEN** an app calls `classLoader.loadClass("android.telephony.TelephonyManager")`
- **THEN** the hook does NOT intercept — the real class is loaded normally

### Requirement: Xposed class blocklist

The hook SHALL block loading of: `de.robv.android.xposed.XposedBridge`, `de.robv.android.xposed.XposedHelpers`, `de.robv.android.xposed.XC_MethodHook`, `io.github.libxposed.api.XposedInterface`, `io.github.libxposed.api.XposedModule`, `com.highcapable.yukihookapi.YukiHookAPI`.

#### Scenario: All known class names blocked

- **WHEN** an app tries to load any class in the blocklist
- **THEN** `ClassNotFoundException` is thrown for each

### Requirement: Hook Runtime.exec() for Xposed file detection

The module SHALL hook `Runtime.exec(String)` to intercept shell commands that check for Xposed-related files (e.g., `/system/lib/libart-xposed.so`).

#### Scenario: App checks for Xposed shared library

- **WHEN** an app runs `Runtime.exec("ls /system/lib/libart-xposed.so")`
- **THEN** the hook intercepts and prevents Xposed file detection

### Requirement: Hook ActivityManager.getRunningServices()

The module SHALL hook `ActivityManager.getRunningServices(int)` to filter out any service entries related to the module or Xposed framework from the returned list.

#### Scenario: Running services filtered

- **WHEN** an app calls `getRunningServices(100)` to check for Xposed services
- **THEN** the returned list does NOT contain entries related to DeviceMasker or Xposed
