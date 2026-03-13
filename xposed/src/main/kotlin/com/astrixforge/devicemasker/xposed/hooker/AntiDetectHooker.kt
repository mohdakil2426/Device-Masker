package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.Log
import com.astrixforge.devicemasker.xposed.DualLog
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback

/**
 * Anti-Detection Hooker — hides Xposed/LSPosed presence from apps.
 *
 * ⚠️ CRITICAL: This hooker MUST be loaded FIRST in [XposedEntry.onPackageLoaded]. If it loads after
 * spoof hooks, there is a brief window where detection is possible.
 *
 * ## Detection vectors covered
 * 1. Stack trace analysis — filters XposedBridge/LSPosed/YukiHookAPI frames from Thread/Throwable
 * 2. ClassLoader.loadClass() — prevents detection via direct classloader queries
 * 3. /proc/self/maps — filters Xposed native library paths from BufferedReader.readLine()
 * 4. PackageManager — hides LSPosed Manager, Magisk, VirtualXposed from PM queries
 *
 * ## API 100 changes
 * - Extends nothing (no YukiBaseHooker)
 * - Static hook(cl, xi, prefs, pkg) factory
 * - @XposedHooker inner classes for callbacks
 * - throwToApp() replaced by invoking NameNotFoundException in
 */
object AntiDetectHooker {

    private const val TAG = "AntiDetectHooker"

    // Class/library/package name fragments to suppress in detection vectors
    private val HIDDEN_CLASS_PATTERNS =
        listOf(
            "de.robv.android.xposed",
            "XposedBridge",
            "XposedHelpers",
            "XC_MethodHook",
            "io.github.lsposed",
            "LSPHooker",
            "LSPosed",
            "com.highcapable.yukihookapi",
            "YukiHookAPI",
            "YukiBaseHooker",
            "EdHooker",
            "EdXposed",
            "com.elderdrivers.riru",
            "rikka.ndk",
            "org.lsposed.lspd",
            "LSPosedService",
            "io.github.libxposed", // Hide libxposed itself too
        )

    private val HIDDEN_LIBRARY_PATTERNS =
        listOf(
            "libxposed",
            "liblspd",
            "libriru",
            "libsandhook",
            "libpine",
            "libwhale",
            "libdobby",
            "libsubstrate",
        )

    private val HIDDEN_PACKAGES =
        setOf(
            "de.robv.android.xposed.installer",
            "org.lsposed.manager",
            "io.github.lsposed.manager",
            "com.topjohnwu.magisk",
            "me.weishu.exp",
            "org.meowcat.edxposed.manager",
        )

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.hiddenPackages = HIDDEN_PACKAGES
        HookState.hiddenClassPatterns = HIDDEN_CLASS_PATTERNS
        HookState.hiddenLibraryPatterns = HIDDEN_LIBRARY_PATTERNS
        DualLog.debug(TAG, "Loading anti-detection hooks for: $pkg")

        hookStackTraces(cl, xi)
        hookClassLoaderLoadClass(cl, xi)
        hookProcMaps(cl, xi)
        hookPackageManager(cl, xi)

        DualLog.debug(TAG, "Anti-detection hooks registered for: $pkg")
    }

    // ─────────────────────────────────────────────────────────────
    // Stack trace hooks (Thread + Throwable)
    // ─────────────────────────────────────────────────────────────

    private fun hookStackTraces(cl: ClassLoader, xi: XposedInterface) {
        // Thread.getStackTrace() — reentrant guard
        try {
            val threadClass = cl.loadClass("java.lang.Thread")
            threadClass
                .getDeclaredMethod("getStackTrace") // reentrant guard
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m, GetThreadStackTraceHooker::class.java) // reentrant guard
                    // deoptimize not needed for getStackTrace — it's never inlined (reentrant
                    // guard)
                }
        } catch (t: Throwable) {
            Log.w(TAG, "Thread.getStackTrace() hook failed: ${t.message}") // reentrant guard
        }
        // Throwable.getStackTrace() — reentrant guard
        try {
            val throwableClass = cl.loadClass("java.lang.Throwable")
            throwableClass
                .getDeclaredMethod("getStackTrace") // reentrant guard
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m, GetThrowableStackTraceHooker::class.java)
                } // reentrant guard
        } catch (t: Throwable) {
            Log.w(TAG, "Throwable.getStackTrace() hook failed: ${t.message}") // reentrant guard
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ClassLoader.loadClass() hook — prevents xposed class enumeration
    // ─────────────────────────────────────────────────────────────

    private fun hookClassLoaderLoadClass(cl: ClassLoader, xi: XposedInterface) {
        try {
            val classLoaderClass = cl.loadClass("java.lang.ClassLoader")
            classLoaderClass
                .getDeclaredMethod("loadClass", String::class.java)
                .also { it.isAccessible = true }
                .let { m -> xi.hook(m, LoadClassHooker::class.java) }
        } catch (t: Throwable) {
            Log.w(TAG, "ClassLoader.loadClass() hook failed: ${t.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // /proc/self/maps hook via BufferedReader.readLine()
    // ─────────────────────────────────────────────────────────────

    private fun hookProcMaps(cl: ClassLoader, xi: XposedInterface) {
        try {
            val brClass = cl.loadClass("java.io.BufferedReader")
            brClass
                .getDeclaredMethod("readLine")
                .also { it.isAccessible = true }
                .let { m -> xi.hook(m, ReadLineHooker::class.java) }
        } catch (t: Throwable) {
            Log.w(TAG, "BufferedReader.readLine() hook failed: ${t.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PackageManager hooks — hide Xposed/root manager packages
    // ─────────────────────────────────────────────────────────────

    private fun hookPackageManager(cl: ClassLoader, xi: XposedInterface) {
        val pmClass = cl.loadClass("android.app.ApplicationPackageManager")

        // getPackageInfo(String, int) + getPackageInfo(String, PackageInfoFlags)
        listOf(arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!)).forEach {
            params ->
            try {
                pmClass
                    .getDeclaredMethod("getPackageInfo", *params)
                    .also { it.isAccessible = true }
                    .let { m -> xi.hook(m, GetPackageInfoHooker::class.java) }
            } catch (_: NoSuchMethodException) {}
        }

        // getApplicationInfo(String, int)
        try {
            pmClass
                .getDeclaredMethod(
                    "getApplicationInfo",
                    String::class.java,
                    Int::class.javaPrimitiveType!!,
                )
                .also { it.isAccessible = true }
                .let { m -> xi.hook(m, GetApplicationInfoHooker::class.java) }
        } catch (t: Throwable) {
            Log.w(TAG, "getApplicationInfo hook failed: ${t.message}")
        }

        // getInstalledPackages(int)
        try {
            pmClass
                .getDeclaredMethod("getInstalledPackages", Int::class.javaPrimitiveType!!)
                .also { it.isAccessible = true }
                .let { m -> xi.hook(m, GetInstalledPackagesHooker::class.java) }
        } catch (t: Throwable) {
            Log.w(TAG, "getInstalledPackages hook failed: ${t.message}")
        }

        // getInstalledApplications(int)
        try {
            pmClass
                .getDeclaredMethod("getInstalledApplications", Int::class.javaPrimitiveType!!)
                .also { it.isAccessible = true }
                .let { m -> xi.hook(m, GetInstalledApplicationsHooker::class.java) }
        } catch (t: Throwable) {
            Log.w(TAG, "getInstalledApplications hook failed: ${t.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Shared state
    // ─────────────────────────────────────────────────────────────

    private val filteringFlag = ThreadLocal<Boolean>()

    internal object HookState {
        @Volatile var hiddenPackages: Set<String> = emptySet()
        @Volatile var hiddenClassPatterns: List<String> = emptyList()
        @Volatile var hiddenLibraryPatterns: List<String> = emptyList()
    }

    internal fun filterStackTrace(stack: Array<StackTraceElement>): Array<StackTraceElement> {
        if (filteringFlag.get() == true) return stack
        filteringFlag.set(true)
        return try {
            val patterns = HookState.hiddenClassPatterns
            stack
                .filterNot { frame ->
                    val cn = frame.className
                    if (cn.startsWith("com.astrixforge.devicemasker")) return@filterNot false
                    if (
                        cn.startsWith("android.") ||
                            cn.startsWith("java.") ||
                            cn.startsWith("kotlin.") ||
                            cn.startsWith("androidx.")
                    )
                        return@filterNot false
                    patterns.any { cn.contains(it, ignoreCase = true) }
                }
                .toTypedArray()
        } finally {
            filteringFlag.set(false)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetThreadStackTraceHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val stack = callback.result as? Array<StackTraceElement> ?: return
                    callback.result = filterStackTrace(stack)
                } catch (t: Throwable) {
                    Log.w("GetThreadStackTraceHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetThrowableStackTraceHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val stack = callback.result as? Array<StackTraceElement> ?: return
                    callback.result = filterStackTrace(stack)
                } catch (t: Throwable) {
                    Log.w("GetThrowableStackTraceHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class LoadClassHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun before(callback: BeforeHookCallback) {
                try {
                    val className = callback.args.firstOrNull() as? String ?: return
                    val patterns = HookState.hiddenClassPatterns
                    if (patterns.any { className.contains(it, ignoreCase = true) }) {
                        callback.throwAndSkip(ClassNotFoundException(className))
                    }
                } catch (t: Throwable) {
                    Log.w("LoadClassHooker", "before() failed: ${t.message}")
                }
            }
        }
    }

    class ReadLineHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val line = callback.result as? String ?: return
                    val patterns = HookState.hiddenLibraryPatterns
                    if (patterns.any { line.contains(it, ignoreCase = true) }) {
                        callback.result = ""
                    }
                } catch (t: Throwable) {
                    Log.w("ReadLineHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetPackageInfoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun before(callback: BeforeHookCallback) {
                try {
                    val pkgName = callback.args.firstOrNull() as? String ?: return
                    if (HookState.hiddenPackages.any { pkgName.equals(it, ignoreCase = true) }) {
                        callback.throwAndSkip(
                            android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        )
                    }
                } catch (t: Throwable) {
                    Log.w("GetPackageInfoHooker", "before() failed: ${t.message}")
                }
            }
        }
    }

    class GetApplicationInfoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun before(callback: BeforeHookCallback) {
                try {
                    val pkgName = callback.args.firstOrNull() as? String ?: return
                    if (HookState.hiddenPackages.any { pkgName.equals(it, ignoreCase = true) }) {
                        callback.throwAndSkip(
                            android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        )
                    }
                } catch (t: Throwable) {
                    Log.w("GetApplicationInfoHooker", "before() failed: ${t.message}")
                }
            }
        }
    }

    class GetInstalledPackagesHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val packages = callback.result as? MutableList<PackageInfo> ?: return
                    packages.removeAll { info ->
                        HookState.hiddenPackages.any {
                            info.packageName.equals(it, ignoreCase = true)
                        }
                    }
                } catch (t: Throwable) {
                    Log.w("GetInstalledPackagesHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetInstalledApplicationsHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val apps = callback.result as? MutableList<ApplicationInfo> ?: return
                    apps.removeAll { info ->
                        HookState.hiddenPackages.any {
                            info.packageName.equals(it, ignoreCase = true)
                        }
                    }
                } catch (t: Throwable) {
                    Log.w("GetInstalledApplicationsHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
