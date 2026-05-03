# Patterns & Examples — libxposed Modern Xposed API

**Example module repo:** https://github.com/libxposed/example  
**Official guide:** https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API

All examples use exact API signatures from the official Javadoc.

---

## 1. Minimal Module (Java)

```java
package com.example.mymodule;

import android.util.Log;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import androidx.annotation.NonNull;
import java.lang.reflect.Method;

public class MyModule extends XposedModule {

    private static final String TAG = "MyModule";
    private static final String TARGET_PKG = "com.example.targetapp";

    // No-arg constructor — do NOT initialize anything here
    public MyModule() {}

    @Override
    public void onModuleLoaded(@NonNull ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Module loaded in: " + param.getProcessName()
            + " | systemServer=" + param.isSystemServer());
    }

    @Override
    public void onPackageReady(@NonNull PackageReadyParam param) {
        if (!TARGET_PKG.equals(param.getPackageName())) return;

        try {
            Class<?> cls = param.getClassLoader()
                .loadClass("com.example.targetapp.SomeClass");
            Method method = cls.getDeclaredMethod("someMethod", String.class, int.class);

            hook(method)
                .setPriority(PRIORITY_DEFAULT)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String arg0 = (String) chain.getArg(0);
                    int    arg1 = (int)    chain.getArg(1);
                    log(Log.DEBUG, TAG, "someMethod called: " + arg0 + ", " + arg1);

                    // Proceed with modified first argument
                    Object result = chain.proceed(new Object[]{"modified_" + arg0, arg1});
                    log(Log.DEBUG, TAG, "someMethod returned: " + result);
                    return result;
                });

        } catch (Exception e) {
            log(Log.ERROR, TAG, "Hook setup failed", e);
        }
    }
}
```

**`META-INF/xposed/java_init.list`**
```
com.example.mymodule.MyModule
```

**`META-INF/xposed/module.prop`**
```properties
minApiVersion=101
targetApiVersion=101
```

**`META-INF/xposed/scope.list`**
```
com.example.targetapp
```

---

## 2. Minimal Module (Kotlin)

```kotlin
package com.example.mymodule

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class MyModule : XposedModule() {

    companion object {
        private const val TAG = "MyModule"
        private const val TARGET_PKG = "com.example.targetapp"
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(Log.INFO, TAG, "Loaded in: ${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != TARGET_PKG) return

        val cls = param.classLoader.loadClass("com.example.targetapp.SomeClass")
        val method = cls.getDeclaredMethod("someMethod", String::class.java)

        hook(method)
            .setPriority(PRIORITY_DEFAULT)
            .intercept { chain ->
                val original = chain.getArg(0) as String
                log(Log.DEBUG, TAG, "Intercepted: $original")
                // proceed() with array of args
                chain.proceed(arrayOf("hooked_$original"))
            }
    }
}
```

---

## 3. Before / After Hook

```java
hook(method).intercept(chain -> {
    // ── BEFORE ───────────────────────────────────────────
    Object thisObj = chain.getThisObject();   // null for static
    String arg = (String) chain.getArg(0);
    log(Log.DEBUG, TAG, "Before: arg=" + arg);

    // ── CALL ORIGINAL ─────────────────────────────────────
    Object result = chain.proceed();

    // ── AFTER ────────────────────────────────────────────
    log(Log.DEBUG, TAG, "After: result=" + result);
    return result;
});
```

---

## 4. Skip Original (Return Fake Value)

```java
hook(method).intercept(chain -> {
    // Do NOT call chain.proceed() — original is never executed
    return Boolean.TRUE;   // always return true
});
```

---

## 5. Modify Arguments

```java
hook(method).intercept(chain -> {
    // chain.getArgs() is IMMUTABLE — pass new array to proceed()
    return chain.proceed(new Object[]{"replaced_arg", 999});
});
```

---

## 6. Replace Return Value

```java
hook(method).intercept(chain -> {
    chain.proceed();              // call original (discard its return value)
    return "replacedResult";      // return something else
});
```

---

## 7. Hook a Constructor

```java
// Note: getThisObject() on a constructor chain returns the NEW instance
// proceed() from a constructor chain returns null (constructors are void)

Class<?> cls = param.getClassLoader().loadClass("com.example.targetapp.MyObject");
Constructor<?> ctor = cls.getConstructor(String.class, int.class);

hook(ctor).intercept(chain -> {
    // Modify constructor args
    Object result = chain.proceed(new Object[]{"patched_name", 0});  // returns null

    // chain.getThisObject() is the newly constructed instance
    Object newInstance = chain.getThisObject();
    Field field = newInstance.getClass().getDeclaredField("secretField");
    field.setAccessible(true);
    field.set(newInstance, "hacked");

    return result; // null
});
```

---

## 8. Change `this` Pointer (proceedWith)

```java
// proceedWith lets you redirect the call to a different instance.
// Only valid for non-static methods — DO NOT use for statics.

hook(method).intercept(chain -> {
    Object alternativeInstance = getSomeOtherInstance();
    // Same args, different 'this':
    return chain.proceedWith(alternativeInstance);
});
```

---

## 9. Hook Static Initializer (`<clinit>`)

```java
// MUST hook before the class is ever loaded — once initialized the hook never fires.
// getThisObject() = null, getArgs() = empty, proceed() = null.

Class<?> cls = param.getClassLoader().loadClass("com.example.targetapp.StaticInit");

hookClassInitializer(cls).intercept(chain -> {
    Object result = chain.proceed();   // runs static initializer

    // Now patch static fields:
    Field secretKey = cls.getDeclaredField("SECRET_KEY");
    secretKey.setAccessible(true);
    secretKey.set(null, "PATCHED");

    return result;  // null
});
```

---

## 10. Hook System Server

```java
// scope.list must contain: system

@Override
public void onSystemServerStarting(@NonNull SystemServerStartingParam param) {
    try {
        ClassLoader cl = param.getClassLoader();
        Class<?> pmClass = cl.loadClass("com.android.server.pm.PackageManagerService");
        Method method = pmClass.getDeclaredMethod("getInstalledPackages", int.class, int.class);

        hook(method).intercept(chain -> {
            log(Log.DEBUG, TAG, "getInstalledPackages intercepted");
            return chain.proceed();
        });
    } catch (Exception e) {
        log(Log.ERROR, TAG, "System server hook failed", e);
    }
}
```

---

## 11. Invoker — Call Original Directly (Skip All Hooks)

```java
// getInvoker() returns an invoker with type = Chain.FULL (default)
// setType(Invoker.Type.ORIGIN) → skip all hooks, call original directly
// Throws checked exceptions: InvocationTargetException, IllegalArgumentException, IllegalAccessException

Method checkLicense = cls.getDeclaredMethod("checkLicense", String.class);
var invoker = getInvoker(checkLicense);
invoker.setType(Invoker.Type.ORIGIN);   // call original, not Invoker.Type.ORIGIN - note: ORIGIN constant is on Type interface

hook(checkLicense).intercept(chain -> {
    Object instance = chain.getThisObject();
    String key = (String) chain.getArg(0);

    // Call original to observe its real return value
    try {
        Object originalResult = invoker.invoke(instance, key);
        log(Log.DEBUG, TAG, "Real result: " + originalResult);
    } catch (Exception e) {
        log(Log.WARN, TAG, "invoker failed", e);
    }

    return true;   // always return true (override real result)
});
```

---

## 12. Invoker — invokeSpecial (Super Call in Hooked Constructor)

```java
// invokeSpecial performs non-virtual dispatch — bypasses overridden methods.
// Useful when you need to call super.someMethod() from inside a hooked constructor.

var superInvoker = getInvoker(superClassMethod);
// invokeSpecial does NOT setType — it always calls the specific class's implementation

hook(ctor).intercept(chain -> {
    Object result = chain.proceed();
    Object newInstance = chain.getThisObject();
    try {
        superInvoker.invokeSpecial(newInstance, "arg");
    } catch (Exception e) {
        log(Log.ERROR, TAG, "invokeSpecial failed", e);
    }
    return result;
});
```

---

## 13. Invoker.Type.Chain — Mid-Chain Invocation

```java
// Skip hooks with priority > 100, include all hooks with priority <= 100:
var invoker = getInvoker(method);
invoker.setType(new Invoker.Type.Chain(100));
Object result = invoker.invoke(instance, arg);
```

---

## 14. Deoptimize for Inline Bypass

```java
// Scenario: targetMethod is inlined by callerMethod → hook on targetMethod never fires.
// Fix: deoptimize callerMethod so it calls targetMethod properly.

Class<?> cls = param.getClassLoader().loadClass("com.example.SomeClass");
Method targetMethod = cls.getDeclaredMethod("isFeatureEnabled");
Method callerMethod = cls.getDeclaredMethod("doSomething");

// Hook first
hook(targetMethod).intercept(chain -> true);

// Then deoptimize the caller
boolean ok = deoptimize(callerMethod);
if (!ok) {
    log(Log.WARN, TAG, "Failed to deoptimize callerMethod — "
        + "consider reinstalling app without uninstalling (forces AOT)");
}
// Tip: use DexKit (https://github.com/LuckyPray/DexKit) to find all callers automatically
```

---

## 15. Named Hooker Class (for Reuse + Unhooking)

```java
public class RootCheckHooker implements XposedInterface.Hooker {
    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        return false;   // deny root detection
    }
}

// Register
HookHandle handle = hook(isRootedMethod)
    .setPriority(PRIORITY_HIGHEST)
    .setExceptionMode(ExceptionMode.PROTECTIVE)
    .intercept(new RootCheckHooker());

// Later, if needed:
handle.unhook();                    // idempotent
Executable hooked = handle.getExecutable();  // inspect what was hooked
```

---

## 16. Priority — Execution Order with Multiple Hooks

```java
// Higher priority number = executes FIRST
// PRIORITY_HIGHEST (Integer.MAX_VALUE) = absolute first

hook(method)
    .setPriority(PRIORITY_HIGHEST)      // runs first
    .intercept(chain -> {
        // pre-process
        return chain.proceed();
    });

hook(method)
    .setPriority(PRIORITY_LOWEST)       // runs last
    .intercept(chain -> {
        Object result = chain.proceed();
        // post-process result
        return result;
    });
```

---

## 17. Reading Remote Preferences in Hooked App

```java
@Override
public void onPackageReady(@NonNull PackageReadyParam param) {
    if (!TARGET_PKG.equals(param.getPackageName())) return;

    // Check remote capability
    if ((getFrameworkProperties() & PROP_CAP_REMOTE) == 0) {
        log(Log.WARN, TAG, "Framework does not support remote prefs");
        return;
    }

    // group name must match what module app UI wrote via XposedService.getRemotePreferences("settings")
    SharedPreferences prefs = getRemotePreferences("settings");
    boolean enabled = prefs.getBoolean("feature_enabled", false);

    if (enabled) {
        // apply hooks...
    }
}
```

---

## 18. Module App — Full Service Setup

```java
// Application.java
public class MyApp extends Application {

    private static volatile XposedService xposedService;

    @Override
    public void onCreate() {
        super.onCreate();
        // Call registerListener only ONCE
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override
            public void onServiceConnected(@NonNull XposedService service) {
                xposedService = service;
                Log.i("MyApp", "Xposed framework: " + service.getFrameworkName()
                    + " v" + service.getFrameworkVersion());
            }

            @Override
            public void onServiceDisconnected() {
                xposedService = null;
            }
        });
    }

    @Nullable
    public static XposedService getXposedService() {
        return xposedService;
    }
}

// MainActivity.java — write settings
XposedService service = MyApp.getXposedService();
if (service != null) {
    SharedPreferences prefs = service.getRemotePreferences("settings");
    prefs.edit()
         .putBoolean("feature_enabled", switchEnabled.isChecked())
         .putString("target_domain", editDomain.getText().toString())
         .apply();
} else {
    Toast.makeText(this, "Xposed framework not connected", Toast.LENGTH_SHORT).show();
}
```

---

## 19. Dynamic Scope Request

```java
XposedService service = MyApp.getXposedService();
if (service != null) {
    List<String> current = service.getScope();
    Log.d(TAG, "Current scope: " + current);

    // Request to add app to scope (prompts user)
    service.requestScope(List.of("com.example.newapp"),
        new XposedService.OnScopeEventListener() {
            @Override
            public void onScopeRequestGranted(@NonNull List<String> packages) {
                Toast.makeText(ctx, "Scope granted for: " + packages, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScopeRequestDenied(@NonNull List<String> packages) {
                Toast.makeText(ctx, "Scope denied", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScopeRequestError(@NonNull List<String> packages, @NonNull String reason) {
                Log.e(TAG, "Scope error: " + reason);
            }
        });

    // Remove
    service.removeScope(List.of("com.example.removedapp"));
}
```

---

## 20. onPackageLoaded vs onPackageReady — When to Use Which

```java
// onPackageLoaded — @RequiresApi(29)
// Use when: you need to hook AppComponentFactory itself,
// or hook something that runs before the full classloader is ready.
@Override
public void onPackageLoaded(@NonNull PackageLoadedParam param) {
    if (!TARGET_PKG.equals(param.getPackageName())) return;
    // param.getDefaultClassLoader() available here (@RequiresApi 29)
    // Hook very early startup code
}

// onPackageReady — available on all API levels
// Use for: most hooks. Classloader is fully ready.
// param.getClassLoader() may differ from getDefaultClassLoader()
// if app uses custom AppComponentFactory.
@Override
public void onPackageReady(@NonNull PackageReadyParam param) {
    if (!TARGET_PKG.equals(param.getPackageName())) return;
    // Hook here for 99% of use cases
    // param.getAppComponentFactory() available (@RequiresApi 28)
}
```

---

## 21. isFirstPackage() — Avoid Duplicate Hooks in Shared Processes

```java
@Override
public void onPackageReady(@NonNull PackageReadyParam param) {
    if (!TARGET_PKG.equals(param.getPackageName())) return;

    // In a process that hosts multiple packages (shared UID apps),
    // onPackageReady fires once per package. Use isFirstPackage() to
    // run one-time init only when the main package is being loaded.
    if (param.isFirstPackage()) {
        // one-time process initialization
        log(Log.INFO, TAG, "First package load in this process");
    }

    // Per-package hooks go here (always)
    hookSpecificFeature(param.getClassLoader());
}
```

---

## Common Mistakes & Fixes

| Mistake | Correct Approach |
|---|---|
| `chain.proceed(List<Object>)` | `chain.proceed(Object[])` — takes array, not List |
| Mutating `chain.getArgs()` | It's immutable — create new `Object[]` and pass to `proceed(args)` |
| `chain.getThisObject()` on static hook | Returns `null` for both static methods AND constructors |
| `invoker.toType(...)` | Correct method is `invoker.setType(...)` |
| `Invoker.Type.Origin.INSTANCE` | Correct is `Invoker.Type.ORIGIN` (static field on `Type` interface) |
| `HookHandle.isHooked()` | Does not exist — only `unhook()` and `getExecutable()` |
| `ModuleLoadedParam.isFirstLoad()` | Does not exist — use `isSystemServer()` and `getProcessName()` |
| `PackageLoadedParam.isFirstLoad()` | Correct method is `isFirstPackage()` |
| `SystemServerStartingParam.getStartParam()` | Does not exist — use `getClassLoader()` directly |
| Constructor with `(XposedInterface, ModuleLoadedParam)` | Modern API uses **no-arg constructor** |
| `ExceptionMode.PROTECTIVE` vs `DEFAULT` | `DEFAULT` is the new third enum value — follows `module.prop` |
| Catching `HookFailedError` | **NEVER catch this** — it is fatal and means a framework bug |
| Registering service listener multiple times | `XposedServiceHelper.registerListener()` should be called **only once** |
| Missing `XposedProvider` in manifest | Service library won't work without it |
| Hook fires but nothing happens (inline) | Call `deoptimize(callerMethod)` or use DexKit to find all callers |
| `ClassNotFoundException` | Use `param.getClassLoader()` — not `Class.forName()` |
| Hook fires in wrong process | Filter by `param.getPackageName()` in every callback |
