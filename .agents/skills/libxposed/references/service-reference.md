# Service Library Reference — io.github.libxposed.service

**Official Javadoc root:** https://libxposed.github.io/service/  
**Package summary:** https://libxposed.github.io/service/io/github/libxposed/service/package-summary.html  
**GitHub repo:** https://github.com/libxposed/service  
**Maven Central:** https://mvnrepository.com/artifact/io.github.libxposed/service

---

## Purpose

The `service` library enables **bidirectional IPC** between:
- The module's **own UI app process** (reads/writes settings, manages scope)
- The **Xposed framework** (delivers data into hooked app processes)

It replaces all legacy `XSharedPreferences` hacks.

---

## Maven Dependency

```kotlin
// build.gradle.kts
implementation("io.github.libxposed:service:<version>")
```

Check latest version: https://mvnrepository.com/artifact/io.github.libxposed/service  
Last known release: March 2026 (check Maven Central for current).

---

## Required Manifest Entry

`XposedProvider` is a `ContentProvider` that the framework uses to connect to the module app.  
**It must be declared in `AndroidManifest.xml` or the service will not work.**

```xml
<provider
    android:name="io.github.libxposed.service.XposedProvider"
    android:authorities="${applicationId}.xposed"
    android:exported="true" />
```

---

## `XposedProvider`

**Javadoc:** https://libxposed.github.io/service/io/github/libxposed/service/XposedProvider.html

```java
public final class XposedProvider extends ContentProvider
```

A `ContentProvider` implementation used internally for framework ↔ module app IPC. You never instantiate or call this directly — just declare it in the manifest. The framework calls into it via Binder/ContentProvider IPC.

---

## `XposedServiceHelper`

**Javadoc:** https://libxposed.github.io/service/io/github/libxposed/service/XposedServiceHelper.html

```java
public final class XposedServiceHelper
```

Registers a listener to receive the `XposedService` binder from the framework.

### Method

```java
// Register a listener to receive the service binder.
// Should be called ONLY ONCE (e.g., in Application.onCreate()).
static void registerListener(XposedServiceHelper.OnServiceListener listener)
```

### `XposedServiceHelper.OnServiceListener`

```java
public interface XposedServiceHelper.OnServiceListener {
    void onServiceConnected(@NonNull XposedService service)
    void onServiceDisconnected()
}
```

Called on the main thread when the framework connects or disconnects.

### Usage Pattern

```java
// In Application.onCreate() or Activity.onCreate():
XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
    @Override
    public void onServiceConnected(@NonNull XposedService service) {
        // Store reference; safe to use all XposedService APIs now
        MyApp.xposedService = service;
    }

    @Override
    public void onServiceDisconnected() {
        MyApp.xposedService = null;
    }
});
```

---

## `XposedService`

> Note: The service library Javadoc at https://libxposed.github.io/service/ has limited published
> detail for `XposedService` itself. The following is sourced from the official repo and guide.

`XposedService` is the main IPC object delivered to `onServiceConnected()`. It represents the live connection to the Xposed framework from the module app process.

### Framework Info

```java
int getApiVersion()
String getFrameworkName()
String getFrameworkVersion()
long getFrameworkVersionCode()
long getFrameworkProperties()
```

### Remote Preferences (read/write from module app UI)

```java
// Get a SharedPreferences backed by the framework's storage.
// Full read/write access from module app UI.
// Read-only in hooked app process via XposedInterface.getRemotePreferences().
SharedPreferences getRemotePreferences(String group)

// Delete a preferences group
void deleteRemotePreferences(String group)
```

`getRemotePreferences()` returns a `RemotePreferences` object which fully implements `SharedPreferences` including `Editor` with `commit()` and `apply()`.

### Remote Files (large data sharing)

```java
// List all file names in the module's shared data directory
String[] listRemoteFiles()

// Open a file for writing from the module app. Creates if not exists.
// name must NOT contain path separators or . / ..
ParcelFileDescriptor openRemoteFile(String name)

// Delete a shared file
boolean deleteRemoteFile(String name)
```

### Scope Management

```java
// Get current list of packages in module scope
List<String> getScope()

// Request to add packages to scope (shows prompt to user)
void requestScope(List<String> packages, OnScopeEventListener callback)

// Remove packages from scope
void removeScope(List<String> packages)
```

### `OnScopeEventListener`

```java
public interface OnScopeEventListener {
    void onScopeRequestGranted(@NonNull List<String> packages)
    void onScopeRequestDenied(@NonNull List<String> packages)
    void onScopeRequestError(@NonNull List<String> packages, @NonNull String reason)
}
```

---

## Full Data Flow Diagram

```
Module App UI Process                      Hooked App Process
─────────────────────────────              ────────────────────────
Application.onCreate()
  └─ XposedServiceHelper.registerListener()

Framework connects → onServiceConnected(service)

service.getRemotePreferences("settings")   XposedInterface.getRemotePreferences("settings")
  .edit()                                  → read-only SharedPreferences
  .putBoolean("enabled", true)
  .apply()
                                           → prefs.getBoolean("enabled", false)

service.openRemoteFile("config.json")      XposedInterface.openRemoteFile("config.json")
  → writable ParcelFileDescriptor          → read-only ParcelFileDescriptor

service.requestScope(["com.example.app"])
  → user sees scope permission dialog
  → onScopeRequestGranted / Denied / Error
```

---

## RemotePreferences — Full SharedPreferences API

```java
XposedService service = MyApp.getService();
SharedPreferences prefs = service.getRemotePreferences("settings");

// ── Read ─────────────────────────────────────────────────────────
boolean flag   = prefs.getBoolean("flag", false);
String  text   = prefs.getString("text", "");
int     count  = prefs.getInt("count", 0);
long    ts     = prefs.getLong("timestamp", 0L);
float   ratio  = prefs.getFloat("ratio", 1.0f);
Set<String> tags = prefs.getStringSet("tags", Collections.emptySet());
Map<String, ?> all = prefs.getAll();
boolean exists = prefs.contains("flag");

// ── Write (from module app UI only) ──────────────────────────────
prefs.edit()
     .putBoolean("flag", true)
     .putString("text", "hello")
     .putInt("count", 42)
     .remove("old_key")
     .apply();    // asynchronous

// OR synchronous:
boolean success = prefs.edit().putBoolean("flag", true).commit();

// ── Change listener ───────────────────────────────────────────────
prefs.registerOnSharedPreferenceChangeListener((p, key) -> {
    // fires when another process modifies the prefs
});
```

---

## Remote Files — Usage

```java
// === Module app UI — write ===
try (ParcelFileDescriptor pfd = service.openRemoteFile("config.json")) {
    try (FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor())) {
        fos.write(jsonBytes);
    }
}

// List files
String[] files = service.listRemoteFiles();

// Delete
boolean deleted = service.deleteRemoteFile("old_config.json");


// === In hooked app process — read (XposedModule.onPackageReady) ===
if ((getFrameworkProperties() & PROP_CAP_REMOTE) != 0) {
    try (ParcelFileDescriptor pfd = openRemoteFile("config.json")) {
        try (FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {
            byte[] data = fis.readAllBytes();
            // parse data
        }
    } catch (FileNotFoundException e) {
        log(Log.WARN, TAG, "config.json not found");
    }
}
```

File `name` constraints (from Javadoc):
- Must NOT contain path separators (`/`, `\`)
- Must NOT be `.` or `..`

---

## Comparison: Legacy vs Modern

| Feature | Legacy (`XSharedPreferences`) | Modern (`service` library) |
|---|---|---|
| Read settings in hooked app | `XSharedPreferences` (world-readable file) | `XposedInterface.getRemotePreferences()` |
| Write settings from module app | Hacky direct file writes, chmod | `XposedService.getRemotePreferences().edit()` |
| Change listener support | ❌ None | ✅ `registerOnSharedPreferenceChangeListener()` |
| Large data sharing | ❌ Not supported | ✅ Remote Files (ParcelFileDescriptor) |
| Dynamic scope management | ❌ Impossible | ✅ `XposedService.requestScope()` |
| Module app hooked by itself | ✅ Was hooked (messy) | ❌ Module app is NOT hooked (clean separation) |
| Security | File-permission-based (fragile) | Binder IPC (framework-enforced) |
| ContentProvider required | ❌ | ✅ `XposedProvider` in manifest |

---

## Deciding What to Use

| Data Type | Tool |
|---|---|
| Simple boolean/string/int settings | `RemotePreferences` via service library |
| Large config files, binary blobs | Remote Files (`openRemoteFile`) |
| Read settings in hooked app (no service dep) | `XposedInterface.getRemotePreferences()` |
| Read files in hooked app (no service dep) | `XposedInterface.openRemoteFile()` |
| Framework info from module app UI | `XposedService.getFrameworkName()` etc. |
| Add/remove scope at runtime | `XposedService.requestScope()` / `removeScope()` |
| Query current scope | `XposedService.getScope()` |
