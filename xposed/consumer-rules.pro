# =============================================================================
# Device Masker — Consumer ProGuard Rules (:xposed module)
# libxposed API 101 edition — YukiHookAPI rules REMOVED.
#
# These rules are merged into :app's R8 run by AGP when :xposed is consumed
# as a library. They protect classes that are loaded via libxposed reflection,
# the AIDL binder mechanism, or Android's ContentProvider discovery.
# =============================================================================

# =============================================================================
# LIBXPOSED API 101 — Hook entry point and hooker infrastructure
# =============================================================================

# XposedModule subclass — entry class declared in META-INF/xposed/java_init.list.
# LSPosed instantiates this class by name via ClassLoader.loadClass().
# API 101: XposedModule uses a no-arg constructor.
-keep class com.astrixforge.devicemasker.xposed.XposedEntry { *; }
-keepclassmembers class com.astrixforge.devicemasker.xposed.XposedEntry {
    public <init>();
    public *;
    static *;
}

# Module-active sentinel — field set by libxposed at load time
-keep class com.astrixforge.devicemasker.XposedModuleActive { *; }

# XposedModule base class — kept for subclassing
-keep class io.github.libxposed.api.XposedModule { *; }

# =============================================================================
# HOOKERS — All hookers and their lambda interceptors
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.hooker.** { *; }
-keepclassmembers class com.astrixforge.devicemasker.xposed.hooker.** {
    public *;
    protected *;
    static *;
}

# =============================================================================
# SERVICE LAYER — AIDL diagnostics service
# DeviceMaskerService extends IDeviceMaskerService.Stub (inner Binder class).
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.service.DeviceMaskerService { *; }
-keepclassmembers class com.astrixforge.devicemasker.xposed.service.DeviceMaskerService {
    *;
}

# SystemServiceHooker registers the AIDL service at boot via AMS hook
-keep class com.astrixforge.devicemasker.xposed.hooker.SystemServiceHooker { *; }

# =============================================================================
# HOOK INFRASTRUCTURE — BaseSpoofHooker, PrefsHelper
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.hooker.BaseSpoofHooker { *; }
-keep class com.astrixforge.devicemasker.xposed.PrefsHelper { *; }

# =============================================================================
# UTILS — Logging and metrics utils (xposed root package)
# =============================================================================
-keep class com.astrixforge.devicemasker.xposed.DualLog { *; }
-keep class com.astrixforge.devicemasker.xposed.HookMetrics { *; }
-keep class com.astrixforge.devicemasker.xposed.PrefsKeys { *; }

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
