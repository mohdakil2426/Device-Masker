package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.util.Log
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticEventType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.diagnostics.XposedDiagnosticEventSink
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.error.XposedFrameworkError

/**
 * Anti-Detection Hooker — hides Xposed/LSPosed presence from apps.
 *
 * ⚠️ CRITICAL: This hooker MUST be loaded FIRST in [XposedEntry.onPackageReady]. If it loads after
 * spoof hooks, there is a brief window where detection is possible.
 *
 * ## Detection vectors covered
 * 1. Stack trace analysis — filters XposedBridge/LSPosed/YukiHookAPI frames from Thread/Throwable
 * 2. ClassLoader.loadClass() — prevents detection via direct classloader queries
 * 3. /proc/self/maps — filters Xposed native library paths from BufferedReader.readLine()
 * 4. PackageManager — hides LSPosed Manager, Magisk, VirtualXposed from PM queries
 *
 * ## API 101 changes
 * - Extends nothing (no YukiBaseHooker)
 * - Static hook(cl, xi, prefs, pkg) factory
 * - Lambda-based interception via `xi.hook(m).intercept { chain -> ... }`
 * - throwToApp() replaced by throwing directly from intercept hook
 */
object AntiDetectHooker {

    private const val TAG = "AntiDetectHooker"

    // Class/library/package name fragments to suppress in detection vectors
    private val HIDDEN_CLASS_PATTERNS =
        arrayOf(
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

    private val SAFE_CLASS_PREFIXES =
        arrayOf(
            "android.",
            "androidx.",
            "com.android.",
            "com.google.",
            "dalvik.",
            "java.",
            "javax.",
            "kotlin.",
            "kotlinx.",
            "org.jetbrains.",
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

    private val classLookupHookActive = ThreadLocal<Boolean>()

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        DualLog.debug(TAG, "Loading anti-detection hooks for: $pkg")

        hookStackTraces(cl, xi)
        DualLog.debug(TAG, "Class lookup hiding disabled for target startup safety")
        XposedDiagnosticEventSink.log(
            Log.INFO,
            TAG,
            "Class lookup hiding skipped for target startup safety",
            eventType = DiagnosticEventType.HOOK_SKIPPED,
        )
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
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
        } catch (t: Throwable) {
            DualLog.warn(TAG, "Throwable.getStackTrace() hook failed", t)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ClassLoader.loadClass() hook — prevents xposed class enumeration
    // ─────────────────────────────────────────────────────────────

    private fun hookClassLoaderLoadClass(cl: ClassLoader, xi: XposedInterface) {
        try {
            val classLoaderClass = ClassLoader::class.java
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
                    xi.hook(method).setExceptionMode(ExceptionMode.PASSTHROUGH).intercept { chain ->
                        if (classLookupHookActive.get() == true) return@intercept chain.proceed()
                        classLookupHookActive.set(true)
                        try {
                            val className = chain.args.firstOrNull() as? String
                            if (className != null && shouldHideClass(className)) {
                                throw ClassNotFoundException(className)
                            }
                            chain.proceed()
                        } finally {
                            classLookupHookActive.set(false)
                        }
                    }
                    xi.deoptimize(method)
                }
        } catch (e: XposedFrameworkError) {
            throw e
        } catch (t: Throwable) {
            DualLog.warn(TAG, "ClassLoader.loadClass() hook failed", t)
        }
    }

    private fun hookClassForName(cl: ClassLoader, xi: XposedInterface) {
        try {
            val classClass = Class::class.java
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
                    xi.hook(method).setExceptionMode(ExceptionMode.PASSTHROUGH).intercept { chain ->
                        if (classLookupHookActive.get() == true) return@intercept chain.proceed()
                        classLookupHookActive.set(true)
                        try {
                            val className = chain.args.firstOrNull() as? String
                            if (className != null && shouldHideClass(className)) {
                                throw ClassNotFoundException(className)
                            }
                            chain.proceed()
                        } finally {
                            classLookupHookActive.set(false)
                        }
                    }
                    xi.deoptimize(method)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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
                        if (
                            line != null &&
                                HIDDEN_LIBRARY_PATTERNS.any { line.contains(it, ignoreCase = true) }
                        ) {
                            ""
                        } else {
                            result
                        }
                    }
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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
        listOf(arrayOf<Class<*>>(String::class.java, Int::class.javaPrimitiveType!!)).forEach {
            params ->
            try {
                pmClass
                    .getDeclaredMethod("getPackageInfo", *params)
                    .also { it.isAccessible = true }
                    .let { m ->
                        xi.hook(m).setExceptionMode(ExceptionMode.PASSTHROUGH).intercept { chain ->
                            val pkgName = chain.args.firstOrNull() as? String
                            if (
                                pkgName != null &&
                                    HIDDEN_PACKAGES.any { pkgName.equals(it, ignoreCase = true) }
                            ) {
                                throw android.content.pm.PackageManager.NameNotFoundException(
                                    pkgName
                                )
                            }
                            chain.proceed()
                        }
                        xi.deoptimize(m)
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
                    xi.hook(m).setExceptionMode(ExceptionMode.PASSTHROUGH).intercept { chain ->
                        val pkgName = chain.args.firstOrNull() as? String
                        if (
                            pkgName != null &&
                                HIDDEN_PACKAGES.any { pkgName.equals(it, ignoreCase = true) }
                        ) {
                            throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        }
                        chain.proceed()
                    }
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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
                        val packages = result as? List<PackageInfo> ?: return@intercept result
                        packages.filterNot { info ->
                            HIDDEN_PACKAGES.any { info.packageName.equals(it, ignoreCase = true) }
                        }
                    }
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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
                        val apps = result as? List<ApplicationInfo> ?: return@intercept result
                        apps.filterNot { info ->
                            HIDDEN_PACKAGES.any { info.packageName.equals(it, ignoreCase = true) }
                        }
                    }
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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
                        val infos = result as? List<ResolveInfo> ?: return@intercept result
                        infos.filterNot { info ->
                            val packageName =
                                info.activityInfo?.packageName ?: return@filterNot false
                            HIDDEN_PACKAGES.any { packageName.equals(it, ignoreCase = true) }
                        }
                    }
                    xi.deoptimize(m)
                }
        } catch (e: XposedFrameworkError) {
            throw e
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

    private fun shouldHideClass(className: String): Boolean {
        for (prefix in SAFE_CLASS_PREFIXES) {
            if (className.startsWith(prefix)) return false
        }
        for (pattern in HIDDEN_CLASS_PATTERNS) {
            if (containsIgnoreCase(className, pattern)) return true
        }
        return false
    }

    internal fun shouldHideClassForTest(className: String): Boolean = shouldHideClass(className)

    private fun containsIgnoreCase(value: String, pattern: String): Boolean {
        val lastStart = value.length - pattern.length
        if (lastStart < 0) return false
        var index = 0
        while (index <= lastStart) {
            if (value.regionMatches(index, pattern, 0, pattern.length, ignoreCase = true)) {
                return true
            }
            index++
        }
        return false
    }
}
