# =============================================================================
# Device Masker — Consumer ProGuard Rules (:xposed module)
# libxposed API 100 edition — YukiHookAPI rules REMOVED.
#
# These rules are merged into :app's R8 run by AGP when :xposed is consumed
# as a library. They protect classes that are loaded via libxposed reflection,
# the AIDL binder mechanism, or Android's ContentProvider discovery.
# =============================================================================

# =============================================================================
# LIBXPOSED API 100 — Hook entry point and hooker infrastructure
# =============================================================================

# XposedModule subclass — entry class declared in META-INF/xposed/java_init.list.
# LSPosed instantiates this class by name via ClassLoader.loadClass().
-keep class com.astrixforge.devicemasker.xposed.XposedEntry { *; }
-keepclassmembers class com.astrixforge.devicemasker.xposed.XposedEntry {
    public <init>(io.github.libxposed.api.XposedInterface, io.github.libxposed.api.XposedModuleInterface$ModuleLoadedParam);
    public *;
    static *;
}

# Module-active sentinel — field set by libxposed at load time
-keep class com.astrixforge.devicemasker.XposedModuleActive { *; }

# @XposedHooker annotated classes — inner static classes with @BeforeInvocation / @AfterInvocation.
# R8 may strip inner classes that appear unreferenced from Kotlin perspective.
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keepclassmembers @io.github.libxposed.api.annotations.XposedHooker class * { *; }

# XposedInterface.Hooker implementations (all @XposedHooker classes implement this)
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }
-keepclassmembers class * implements io.github.libxposed.api.XposedInterface$Hooker {
    public static *;
}

# XposedModule base class — kept for subclassing
-keep class io.github.libxposed.api.XposedModule { *; }

# =============================================================================
# HOOKERS — All hookers and their @XposedHooker inner classes
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.hooker.** { *; }
-keepclassmembers class com.astrixforge.devicemasker.xposed.hooker.** {
    public *;
    protected *;
    static *;
}

# =============================================================================
# SERVICE LAYER — AIDL diagnostics service + ContentProvider bridge
# DeviceMaskerService extends IDeviceMaskerService.Stub (inner Binder class).
# ServiceBridge is declared in AndroidManifest as a ContentProvider.
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.service.DeviceMaskerService { *; }
-keepclassmembers class com.astrixforge.devicemasker.xposed.service.DeviceMaskerService {
    *;
}

# ServiceBridge ContentProvider is registered dynamically in system_server
-keep class com.astrixforge.devicemasker.xposed.service.ServiceBridge { *; }

# SystemServiceHooker registers the AIDL service at boot via AMS hook
-keep class com.astrixforge.devicemasker.xposed.hooker.SystemServiceHooker { *; }

# =============================================================================
# HOOK INFRASTRUCTURE — BaseSpoofHooker, PrefsHelper, DeoptimizeManager
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.hooker.BaseSpoofHooker { *; }
-keep class com.astrixforge.devicemasker.xposed.PrefsHelper { *; }
-keep class com.astrixforge.devicemasker.xposed.DeoptimizeManager { *; }

# =============================================================================
# UTILS — ClassCache LRU, metrics, etc. (xposed/utils package)
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.utils.** { *; }

# =============================================================================
# LIBXPOSED SERVICE — ModulePreferences for writing RemotePreferences from :app
# =============================================================================
-keep class io.github.libxposed.service.** { *; }
-keep interface io.github.libxposed.service.** { *; }
-dontwarn io.github.libxposed.**

# =============================================================================
# BINDER INFRASTRUCTURE — Required for AIDL stubs/proxies
# =============================================================================
-keepclassmembers class * extends android.os.Binder {
    public static ** asInterface(android.os.IBinder);
    public android.os.IBinder asBinder();
}

# =============================================================================
# SUPPRESS LEGACY XPOSED WARNINGS
# The libxposed API jar stubs may reference old de.robv classes for ABI compat.
# =============================================================================
-dontwarn de.robv.android.xposed.**
-dontwarn com.highcapable.**
