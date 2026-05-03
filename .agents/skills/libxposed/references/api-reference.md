# API Reference — io.github.libxposed.api

**Official Javadoc root:** https://libxposed.github.io/api/  
**Package summary:** https://libxposed.github.io/api/io/github/libxposed/api/package-summary.html  
**Error package:** https://libxposed.github.io/api/io/github/libxposed/api/error/package-summary.html

---

## Class Hierarchy

```
Object
└── XposedInterfaceWrapper  (implements XposedInterface)
    └── XposedModule        (implements XposedModuleInterface)
```

---

## `XposedModule`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedModule.html

```java
public abstract class XposedModule
    extends XposedInterfaceWrapper
    implements XposedModuleInterface
```

Super class all module entry classes must extend. Instantiated **exactly once per process** via its no-arg constructor. Do NOT do initialization work before `onModuleLoaded()` is called.

**Constructor:**
```java
public XposedModule()   // no-arg; do NOT initialize anything here
```

**Constants (inherited from `XposedInterface`):**
```java
int API_101 = 101                        // first stable API version
int LIB_API                              // static compile-time library version
int PRIORITY_HIGHEST = Integer.MAX_VALUE // first to execute in hook chain
int PRIORITY_DEFAULT = 50
int PRIORITY_LOWEST  = Integer.MIN_VALUE // last to execute in hook chain
long PROP_CAP_SYSTEM                     // framework supports hooking system server
long PROP_CAP_REMOTE                     // framework supports remote prefs + files
long PROP_RT_API_PROTECTION              // API access via reflection/dynamic load blocked
                                         // PROP_RT_ flags may change between launches
```

All methods come from `XposedInterfaceWrapper` (all `final`) and `XposedModuleInterface` (all `default`).

---

## `XposedInterfaceWrapper`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterfaceWrapper.html

All methods are **`final`**. The framework cannot override them.

```java
// Framework attachment — MODULES MUST NEVER CALL THIS
public final void attachFramework(@NonNull XposedInterface base)

// ── Framework / API info ──────────────────────────────────────────
public final int getApiVersion()                 // runtime API version
public final @NonNull String getFrameworkName()
public final @NonNull String getFrameworkVersion()
public final long getFrameworkVersionCode()
public final long getFrameworkProperties()       // PROP_RT_ bits may change per launch

// ── Module info ───────────────────────────────────────────────────
public final @NonNull ApplicationInfo getModuleApplicationInfo()

// ── Hooking ───────────────────────────────────────────────────────
public final @NonNull HookBuilder hook(@NonNull Executable origin)
public final @NonNull HookBuilder hookClassInitializer(@NonNull Class<?> origin)
public final boolean deoptimize(@NonNull Executable executable)

// ── Invokers (bypass access checks) ──────────────────────────────
public final @NonNull Invoker<?, Method>  getInvoker(@NonNull Method method)
public final @NonNull <T> CtorInvoker<T> getInvoker(@NonNull Constructor<T> constructor)

// ── Logging ───────────────────────────────────────────────────────
// priority: android.util.Log constants (DEBUG, INFO, WARN, ERROR, etc.)
public final void log(int priority, @Nullable String tag, @NonNull String msg)
public final void log(int priority, @Nullable String tag, @NonNull String msg,
                      @Nullable Throwable tr)

// ── Remote data (requires PROP_CAP_REMOTE) ────────────────────────
// name = group name for preferences
// In hooked apps: READ-ONLY. Write from module app UI via service library.
public final @NonNull SharedPreferences getRemotePreferences(@NonNull String name)

// List all files in module's shared data directory
public final @NonNull String[] listRemoteFiles()

// Open file READ-ONLY in hooked app. name must NOT contain / \ . or ..
// Throws FileNotFoundException if file does not exist or path is forbidden
public final @NonNull ParcelFileDescriptor openRemoteFile(@NonNull String name)
    throws FileNotFoundException
```

---

## `XposedModuleInterface`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.html

All methods have **default (empty)** implementations — override only what you need.

```java
// Guaranteed to be called EXACTLY ONCE per process.
default void onModuleLoaded(@NonNull ModuleLoadedParam param)

// Default classloader ready; BEFORE AppComponentFactory.
// May fire MULTIPLE TIMES per process (once per package). @RequiresApi(29)
default void onPackageLoaded(@NonNull PackageLoadedParam param)

// AppComponentFactory has instantiated the app classloader.
// May fire MULTIPLE TIMES per process (once per package).
default void onPackageReady(@NonNull PackageReadyParam param)

// System server is ready to start critical services.
// Only delivered to modules in system scope.
default void onSystemServerStarting(@NonNull SystemServerStartingParam param)
```

---

## `ModuleLoadedParam`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.ModuleLoadedParam.html

```java
public static interface XposedModuleInterface.ModuleLoadedParam
```

State is fixed at load time; will not be updated.

```java
@NonNull String getProcessName()   // name of the current process
boolean isSystemServer()           // true if this process is system server
```

---

## `PackageLoadedParam`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.PackageLoadedParam.html

```java
public static interface XposedModuleInterface.PackageLoadedParam
// Direct known subinterfaces: PackageReadyParam
```

```java
@NonNull String getPackageName()
@NonNull ApplicationInfo getApplicationInfo()
boolean isFirstPackage()                        // true if this is the first/main package in this process

@RequiresApi(29)
@NonNull ClassLoader getDefaultClassLoader()    // loads app code + AppComponentFactory
```

---

## `PackageReadyParam`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.PackageReadyParam.html

```java
public static interface XposedModuleInterface.PackageReadyParam
    extends PackageLoadedParam
```

Inherits all of `PackageLoadedParam`. Additionally:

```java
// May differ from getDefaultClassLoader() if custom AppComponentFactory creates its own classloader
@NonNull ClassLoader getClassLoader()

@RequiresApi(28)
@NonNull AppComponentFactory getAppComponentFactory()
```

---

## `SystemServerStartingParam`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.SystemServerStartingParam.html

```java
public static interface XposedModuleInterface.SystemServerStartingParam
```

State is fixed at load time; will not be updated.

```java
@NonNull ClassLoader getClassLoader()   // system server class loader
```

---

## `XposedInterface.HookBuilder`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.HookBuilder.html

```java
public static interface XposedInterface.HookBuilder
```

Obtained from `hook(executable)`. Methods return `this` for chaining (except `intercept()`).

```java
// Default: PRIORITY_DEFAULT (50). Higher = executes earlier in chain.
HookBuilder setPriority(int priority)

// Default: ExceptionMode.DEFAULT
HookBuilder setExceptionMode(@NonNull ExceptionMode mode)

// Registers the hook and returns a handle. TERMINAL — call last.
// Throws IllegalArgumentException if origin is framework-internal,
//        Constructor.newInstance(), or hooker is invalid.
// Throws HookFailedError (FATAL) on framework internal error — do NOT catch.
@NonNull HookHandle intercept(@NonNull Hooker hooker)
```

---

## `XposedInterface.Hooker`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Hooker.html

```java
@FunctionalInterface
public static interface XposedInterface.Hooker
```

```java
Object intercept(Chain chain) throws Throwable
```

Implement as lambda, anonymous class, or named class. Return the final result value. Exceptions are handled per `ExceptionMode`.

---

## `XposedInterface.Chain`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Chain.html

```java
public static interface XposedInterface.Chain
```

> Chain objects **cannot be shared between threads** or **reused** after `intercept()` returns.

```java
@NonNull Executable getExecutable()     // the hooked Method or Constructor

// 'this' pointer. null for BOTH static methods AND constructors.
Object getThisObject()

// IMMUTABLE list — do not attempt to mutate it
@NonNull List<Object> getArgs()

// Single argument by index
// Throws IndexOutOfBoundsException or ClassCastException
Object getArg(int index)

// ── proceed() variants ────────────────────────────────────────────
// All variants return null for void methods and constructors.
// Exceptions from any interceptor or original always propagate.

// Same args + same 'this'
Object proceed() throws Throwable

// New args array + same 'this'
Object proceed(@NonNull Object[] args) throws Throwable

// Same args + new 'this' pointer — DO NOT call from static interceptors
Object proceedWith(@NonNull Object thisObject) throws Throwable

// New args + new 'this' — DO NOT call from static interceptors
Object proceedWith(@NonNull Object thisObject, @NonNull Object[] args) throws Throwable
```

---

## `XposedInterface.HookHandle`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.HookHandle.html

```java
public static interface XposedInterface.HookHandle
```

Exactly **two methods**:

```java
// Cancels the hook. IDEMPOTENT — safe to call multiple times.
void unhook()

// Gets the hooked Method or Constructor.
@NonNull Executable getExecutable()
```

---

## `XposedInterface.ExceptionMode`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.ExceptionMode.html

```java
public static enum XposedInterface.ExceptionMode
```

Three constants (ordered by declaration):

```java
DEFAULT      // Follows global exceptionMode in module.prop. Defaults to PROTECTIVE if unset.

PROTECTIVE   // Hooker exception caught + logged; execution continues as if no hook exists.
             // Before proceed(): chain continues without this hook.
             // After proceed(): returns the proceeded value/exception.
             // Exceptions FROM proceed() always propagate regardless.
             // Recommended for production/release.

PASSTHROUGH  // Any hooker exception propagates to the caller as usual.
             // Recommended for debugging.
```

---

## `XposedInterface.Invoker<T, U>`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Invoker.html

```java
public static interface XposedInterface.Invoker<
    T extends XposedInterface.Invoker<T,U>,
    U extends Executable>
// Known subinterface: CtorInvoker<T>
```

Default type after `getInvoker()` = `Invoker.Type.Chain.FULL` (full hook chain).

```java
// Set invocation type. Mutates and returns self for chaining.
T setType(@NonNull Invoker.Type type)

// Virtual dispatch through the hook chain determined by invoker's type.
// thisObject = null for static methods.
// Throws InvocationTargetException, IllegalArgumentException, IllegalAccessException
Object invoke(Object thisObject, Object... args)
    throws InvocationTargetException, IllegalArgumentException, IllegalAccessException

// Non-virtual dispatch — like JNI CallNonVirtualMethod.
// Bypasses overridden methods in subclasses; calls the method as defined on the specific class.
// Useful for calling super.xxx() inside hooked constructors.
// Throws InvocationTargetException, IllegalArgumentException, IllegalAccessException
Object invokeSpecial(@NonNull Object thisObject, Object... args)
    throws InvocationTargetException, IllegalArgumentException, IllegalAccessException
```

---

## `XposedInterface.CtorInvoker<T>`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.CtorInvoker.html

```java
public static interface XposedInterface.CtorInvoker<T>
    extends XposedInterface.Invoker<CtorInvoker<T>, Constructor<T>>
```

Constructor-specific invoker. Inherits `setType()`, `invoke()`, `invokeSpecial()`.

---

## `XposedInterface.Invoker.Type` (sealed hierarchy)

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Invoker.Type.html

```java
public static sealed interface XposedInterface.Invoker.Type
    permits XposedInterface.Invoker.Type.Origin,
            XposedInterface.Invoker.Type.Chain
```

Static convenience constant on the `Type` interface itself:
```java
static final Invoker.Type.Origin ORIGIN   // shortcut — same as new Origin()
```

### `Invoker.Type.Origin` (record)

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Invoker.Type.Origin.html

```java
public static final record XposedInterface.Invoker.Type.Origin
    implements XposedInterface.Invoker.Type
```

Skips ALL hooks; invokes the original executable directly. Use `Invoker.Type.ORIGIN` constant — no need to construct manually.

### `Invoker.Type.Chain` (record)

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Invoker.Type.Chain.html

```java
public static record XposedInterface.Invoker.Type.Chain(int maxPriority)
    implements XposedInterface.Invoker.Type
```

Invokes starting from the **middle** of the hook chain, skipping all hooks with priority **higher than** `maxPriority`.

```java
// Full chain (all hooks run) — this is the default from getInvoker()
public static final Invoker.Type.Chain FULL

// Start mid-chain: skip hooks with priority > maxPriority
public Chain(int maxPriority)

public int maxPriority()   // record accessor
```

---

## `io.github.libxposed.api.error`

**Javadoc:** https://libxposed.github.io/api/io/github/libxposed/api/error/package-summary.html

### `XposedFrameworkError`

```java
public class XposedFrameworkError extends RuntimeException
```

Base class for all Xposed framework errors. Indicates the framework function is broken.

### `HookFailedError`

```java
public class HookFailedError extends XposedFrameworkError
```

Thrown when a hook fails due to a **framework internal error**. This is **FATAL** — do NOT catch this in module code. Report to the framework maintainers.

---

## `hookClassInitializer()` — Special Behaviour

Per official Javadoc:

- `getExecutable()` → a **synthetic `Method`** representing the static initializer
- `getThisObject()` → always `null`
- `getArgs()` → always **empty list**
- `proceed()` → always returns `null`
- Hook fires **only if the class is NOT yet initialized** at the time of hooking

---

## `deoptimize()` — How It Works

Per official Javadoc:

> By deoptimizing a method, the runtime will fall back to calling all callees without inlining.
> When a short hooked method B is invoked by method A, the hook on B may not fire if A has
> inlined B. Deoptimizing A forces it to call the hooked B properly.
>
> Generally you need to find **all** callers of your hooked callee — which is difficult without
> tools like [DexKit](https://github.com/LuckyPray/DexKit). If you cannot enumerate all callers,
> consider changing the hook point, or deoptimizing the whole app by reinstalling it without
> uninstalling first (forces AOT recompilation).
>
> Returns `true` on success, `false` on failure.
