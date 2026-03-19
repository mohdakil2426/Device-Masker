package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import com.astrixforge.devicemasker.xposed.DualLog
import io.github.libxposed.api.XposedInterface

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
 * - Lambda-based interception
 * - throwToApp() replaced by throwing directly from intercept hook
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
        DualLog.debug(TAG, "Loading anti-detection hooks for: $pkg")

        hookStackTraces(cl, xi)
        hookClassLoaderLoadClass(cl, xi)
        hookClassForName(cl, xi)
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
                .getDeclaredMethod("getStackTrace")
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val stack = result as? Array<StackTraceElement> ?: return@intercept result
                        filterStackTrace(stack)
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "Thread.getStackTrace() hook failed", t)
        }
        // Throwable.getStackTrace() — reentrant guard
        try {
            val throwableClass = cl.loadClass("java.lang.Throwable")
            throwableClass
                .getDeclaredMethod("getStackTrace")
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val stack = result as? Array<StackTraceElement> ?: return@intercept result
                        filterStackTrace(stack)
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "Throwable.getStackTrace() hook failed", t)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ClassLoader.loadClass() hook — prevents xposed class enumeration
    // ─────────────────────────────────────────────────────────────

    private fun hookClassLoaderLoadClass(cl: ClassLoader, xi: XposedInterface) {
        try {
            val classLoaderClass = cl.loadClass("java.lang.ClassLoader")
            classLoaderClass.declaredMethods
                .filter {
                    it.name == "loadClass" &&
                        (it.parameterTypes.contentEquals(arrayOf(String::class.java)) ||
                            it.parameterTypes.contentEquals(
                                arrayOf(String::class.java, Boolean::class.javaPrimitiveType!!)
                            ))
                }
                .forEach { method ->
                    method.isAccessible = true
                    xi.hook(method).intercept { chain ->
                        val className = chain.args.firstOrNull() as? String
                        if (className != null && HIDDEN_CLASS_PATTERNS.any { className.contains(it, ignoreCase = true) }) {
                            throw ClassNotFoundException(className)
                        }
                        chain.proceed()
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "ClassLoader.loadClass() hook failed", t)
        }
    }

    private fun hookClassForName(cl: ClassLoader, xi: XposedInterface) {
        try {
            val classClass = cl.loadClass("java.lang.Class")
            classClass.declaredMethods
                .filter {
                    it.name == "forName" &&
                        (it.parameterTypes.contentEquals(arrayOf(String::class.java)) ||
                            it.parameterTypes.contentEquals(
                                arrayOf(
                                    String::class.java,
                                    Boolean::class.javaPrimitiveType!!,
                                    ClassLoader::class.java,
                                )
                            ))
                }
                .forEach { method ->
                    method.isAccessible = true
                    xi.hook(method).intercept { chain ->
                        val className = chain.args.firstOrNull() as? String
                        if (className != null && HIDDEN_CLASS_PATTERNS.any { className.contains(it, ignoreCase = true) }) {
                            throw ClassNotFoundException(className)
                        }
                        chain.proceed()
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "Class.forName() hook failed", t)
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
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        val line = result as? String
                        if (line != null && HIDDEN_LIBRARY_PATTERNS.any { line.contains(it, ignoreCase = true) }) {
                            ""
                        } else {
                            result
                        }
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "BufferedReader.readLine() hook failed", t)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PackageManager hooks — hide Xposed/root manager packages
    // ─────────────────────────────────────────────────────────────

    private fun hookPackageManager(cl: ClassLoader, xi: XposedInterface) {
        val pmClass = cl.loadClass("android.app.ApplicationPackageManager")

        // getPackageInfo(String, int) + getPackageInfo(String, PackageInfoFlags)
        listOf(arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!)).forEach { params ->
            try {
                pmClass
                    .getDeclaredMethod("getPackageInfo", *params)
                    .also { it.isAccessible = true }
                    .let { m ->
                        xi.hook(m).intercept { chain ->
                            val pkgName = chain.args.firstOrNull() as? String
                            if (pkgName != null && HIDDEN_PACKAGES.any { pkgName.equals(it, ignoreCase = true) }) {
                                throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                            }
                            chain.proceed()
                        }
                    }
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
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val pkgName = chain.args.firstOrNull() as? String
                        if (pkgName != null && HIDDEN_PACKAGES.any { pkgName.equals(it, ignoreCase = true) }) {
                            throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        }
                        chain.proceed()
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "getApplicationInfo hook failed", t)
        }

        // getInstalledPackages(int)
        try {
            pmClass
                .getDeclaredMethod("getInstalledPackages", Int::class.javaPrimitiveType!!)
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val packages = result as? MutableList<PackageInfo> ?: return@intercept result
                        packages.removeAll { info ->
                            HIDDEN_PACKAGES.any { info.packageName.equals(it, ignoreCase = true) }
                        }
                        packages
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "getInstalledPackages hook failed", t)
        }

        // getInstalledApplications(int)
        try {
            pmClass
                .getDeclaredMethod("getInstalledApplications", Int::class.javaPrimitiveType!!)
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val apps = result as? MutableList<ApplicationInfo> ?: return@intercept result
                        apps.removeAll { info ->
                            HIDDEN_PACKAGES.any { info.packageName.equals(it, ignoreCase = true) }
                        }
                        apps
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "getInstalledApplications hook failed", t)
        }

        try {
            pmClass
                .getDeclaredMethod(
                    "queryIntentActivities",
                    android.content.Intent::class.java,
                    Int::class.javaPrimitiveType!!,
                )
                .also { it.isAccessible = true }
                .let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val infos = result as? MutableList<ResolveInfo> ?: return@intercept result
                        infos.removeAll { info ->
                            val packageName = info.activityInfo?.packageName ?: return@removeAll false
                            HIDDEN_PACKAGES.any { packageName.equals(it, ignoreCase = true) }
                        }
                        infos
                    }
                }
        } catch (t: Throwable) {
            DualLog.warn(TAG, "queryIntentActivities hook failed", t)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Shared state
    // ─────────────────────────────────────────────────────────────

    private val filteringFlag = ThreadLocal<Boolean>()

    internal fun filterStackTrace(stack: Array<StackTraceElement>): Array<StackTraceElement> {
        if (filteringFlag.get() == true) return stack
        filteringFlag.set(true)
        return try {
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
                    HIDDEN_CLASS_PATTERNS.any { cn.contains(it, ignoreCase = true) }
                }
                .toTypedArray()
        } finally {
            filteringFlag.set(false)
        }
    }
}
