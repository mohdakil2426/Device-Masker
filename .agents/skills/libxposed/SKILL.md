---
name: libxposed
description: >
  Expert guide for developing Android Xposed modules using the modern libxposed API (API 101+).
  Covers the full development lifecycle: project setup with Gradle and Maven Central, module
  entry registration, OkHttp-style interceptor chain hooks (XposedInterface, Hooker, Chain),
  all lifecycle callbacks (onModuleLoaded, onPackageLoaded, onPackageReady,
  onSystemServerStarting), the Invoker system (invoke, invokeSpecial, setType, Origin, Chain),
  scope management, deoptimization for inline bypasses, remote preferences and remote file
  sharing, and the service/helper companion libraries. Always use this skill when the user
  asks about: writing an Xposed module, hooking Android methods, libxposed API, LSPosed module
  development, XposedModule, XposedInterface, Hooker, Chain.intercept, java_init.list,
  module.prop, scope.list, XposedService, XposedBridge migration, Maven Central dependency,
  compileOnly libxposed, or anything related to Xposed framework development for Android.
---

# libxposed Modern Xposed API Skill

## Official Resources

| Resource | URL |
|---|---|
| API Javadoc | https://libxposed.github.io/api/ |
| Service Javadoc | https://libxposed.github.io/service/ |
| API GitHub repo | https://github.com/libxposed/api |
| Service GitHub repo | https://github.com/libxposed/service |
| Helper GitHub repo | https://github.com/libxposed/helper |
| Example module | https://github.com/libxposed/example |
| LSPosed wiki guide | https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API |
| Maven Central — api | https://central.sonatype.com/artifact/io.github.libxposed/api |
| MvnRepository — all | https://mvnrepository.com/artifact/io.github.libxposed |

## Reference Files in This Skill

| File | When to read |
|---|---|
| `references/project-setup.md` | New project, Gradle, Maven Central coords, META-INF files, manifest |
| `references/api-reference.md` | All classes/interfaces with exact signatures from official Javadoc |
| `references/service-reference.md` | XposedService, XposedServiceHelper, XposedProvider, RemotePreferences |
| `references/patterns-and-examples.md` | Full runnable Java & Kotlin examples covering every major scenario |

---

## Quick Mental Model

```
Module APK
├── META-INF/xposed/
│   ├── java_init.list      ← "com.example.MyModule" (one FQN per line)
│   ├── native_init.list    ← native entries (optional)
│   ├── module.prop         ← minApiVersion=101, targetApiVersion=101
│   └── scope.list          ← target package names (one per line)
└── src/main/java/
    └── MyModule.java       ← extends XposedModule (no-arg constructor)
```

The framework instantiates `MyModule` exactly **once per process** via its **no-arg constructor**, then calls:
1. `attachFramework(XposedInterface)` — done automatically by framework; **modules must NOT call this**
2. `onModuleLoaded(ModuleLoadedParam)` — first safe place to initialize anything
3. `onPackageLoaded(PackageLoadedParam)` — default classloader ready, before `AppComponentFactory` (API 29+)
4. `onPackageReady(PackageReadyParam)` — full app classloader ready → best place for most hooks
5. `onSystemServerStarting(SystemServerStartingParam)` — system server scope only

---

## Maven Central — Artifacts

Primary distribution is **Maven Central**. GitHub Packages is an alternative but requires authentication.

| Artifact ID | Scope for modules | Description |
|---|---|---|
| `api` | **`compileOnly`** | Core hook/lifecycle API — framework provides at runtime |
| `service` | `implementation` | Module app ↔ framework IPC (XposedService) |
| `helper` | `implementation` | XposedHelpers replacement utility library |
| `helper-ktx` | `implementation` | Kotlin extensions for helper |

```kotlin
// build.gradle.kts — module developer
dependencies {
    compileOnly("io.github.libxposed:api:101.0.1")
    // Optional (for module UI app IPC):
    implementation("io.github.libxposed:service:<version>")
    // Optional (reflection helpers):
    implementation("io.github.libxposed:helper:<version>")
    implementation("io.github.libxposed:helper-ktx:<version>")
}

// build.gradle.kts — framework developer only
dependencies {
    implementation("io.github.libxposed:api:101.0.1")
}
```

Check latest versions at:
- https://central.sonatype.com/artifact/io.github.libxposed/api
- https://mvnrepository.com/artifact/io.github.libxposed

> **Important:** `api` **must** be `compileOnly` for modules. The framework injects the real implementation at runtime. Making it `implementation` would bloat the APK with stub code that does nothing.

---

## Core Hook Pattern

```java
// Obtain from XposedInterface (inherited by XposedModule):
HookHandle handle = hook(method)
    .setPriority(PRIORITY_DEFAULT)
    .setExceptionMode(ExceptionMode.DEFAULT)
    .intercept(chain -> {
        Object arg0   = chain.getArg(0);        // by index
        List<Object> args = chain.getArgs();    // immutable list
        Object thiz   = chain.getThisObject();  // null for static methods

        Object result = chain.proceed();                    // same args + same this
        // Object result = chain.proceed(new Object[]{…}); // different args
        // Object result = chain.proceedWith(otherObj);    // different this
        // Object result = chain.proceedWith(obj, args);   // different both
        return result;
    });

handle.unhook();            // remove hook (idempotent)
handle.getExecutable();     // the hooked Method or Constructor
```

---

## Critical Facts (corrections from legacy knowledge)

| Topic | Correct Fact |
|---|---|
| `chain.proceed(args)` | Takes `Object[]` array — **NOT** `List<Object>` |
| `chain.getArgs()` | Returns **immutable** `List<Object>` |
| `chain.getArg(int)` | Convenience accessor; throws `IndexOutOfBoundsException` or `ClassCastException` |
| `chain.proceedWith(thisObject, args)` | Changes `this`; static interceptors must NOT call |
| `HookHandle` methods | Only `unhook()` (idempotent) and `getExecutable()` |
| `Invoker` type mutation | `setType(Invoker.Type)` — **NOT** `toType()` |
| `Invoker.Type.ORIGIN` | Static constant on the `Type` interface (shortcut for `Origin` record) |
| `Invoker.Type.Chain.FULL` | Static field on `Chain` record — full hook chain |
| `invoker.invokeSpecial()` | Non-virtual dispatch — useful for `super.xxx()` in hooked constructors |
| `invoker.invoke()` | Throws `InvocationTargetException`, `IllegalArgumentException`, `IllegalAccessException` |
| `ExceptionMode.DEFAULT` | Third enum constant — follows `module.prop`; defaults to `PROTECTIVE` |
| `XposedModule` constructor | **No-arg** — `public XposedModule()` |
| `ModuleLoadedParam` | Has `getProcessName()` and `isSystemServer()` — NO `isFirstLoad()` |
| `PackageLoadedParam` | Has `isFirstPackage()` — NOT `isFirstLoad()` |
| `PackageReadyParam` | Extends `PackageLoadedParam`; adds `getClassLoader()` and `getAppComponentFactory()` |
| `SystemServerStartingParam` | Only `getClassLoader()` — no `getStartParam()` |
| `onPackageLoaded` | Annotated `@RequiresApi(29)` |
| `getAppComponentFactory()` | Annotated `@RequiresApi(28)` |
| `getDefaultClassLoader()` | Annotated `@RequiresApi(29)` |
| Scope filter | Modules MUST filter by `param.getPackageName()` — may receive extra callbacks |
| `XposedServiceHelper.registerListener()` | Should be called **only once** |

---

## ExceptionMode — Three Values

| Value | Behavior |
|---|---|
| `DEFAULT` | Follows global `exceptionMode` set in `module.prop`; defaults to `PROTECTIVE` if unset |
| `PROTECTIVE` | Exception before `proceed()` → framework continues chain without hook. Exception after `proceed()` → returns the proceeded value. Exceptions from `proceed()` itself always propagate |
| `PASSTHROUGH` | All hooker exceptions propagate to caller as usual. Recommended for debugging |

---

## Invoker.Type Hierarchy

```
Invoker.Type  (sealed interface)
 ├── static final ORIGIN         → shortcut constant for Origin record
 ├── Origin (record)             → skips ALL hooks, calls original directly
 └── Chain (record, int maxPriority)
     ├── Chain.FULL              → full chain (default from getInvoker())
     └── new Chain(priority)     → skips hooks with priority > maxPriority
```

---

## Scope System

- `scope.list` — one package name per line
- `system` → special virtual name for system server process  
- `android` → targets the android framework `:ui` process (valid scope target)
- Packages whose components ALL run in system server → use `system`, not the package name
- `staticScope=true` in `module.prop` → users cannot manually add apps outside the list

For full project file structure and Gradle setup see `references/project-setup.md`.
For all official class signatures see `references/api-reference.md`.
For complete examples see `references/patterns-and-examples.md`.
For the service IPC library see `references/service-reference.md`.
