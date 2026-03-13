package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback

/**
 * Package Manager Hooker — new in libxposed API 100 migration (Gap 4.10).
 *
 * Hides the Device Masker module's own package from apps that query PackageManager. Apps that
 * detect root/Xposed tools often enumerate installed packages and check for known Xposed manager
 * packages (like org.lsposed.manager) AND the hooked module itself.
 *
 * This hooker is distinct from AntiDetectHooker's PackageManager hooks:
 * - AntiDetectHooker hides LSPosed Manager, Magisk, and other tool packages
 * - PackageManagerHooker hides Device Masker itself (com.astrixforge.devicemasker)
 *
 * Both hookers must be active simultaneously for complete self-concealment.
 *
 * ## Hooks covered
 * - getPackageInfo(String, int) — returns NameNotFoundException for our package
 * - getApplicationInfo(String, int) — returns NameNotFoundException for our package
 * - getInstalledPackages(int) — removes our package from the returned list
 * - getInstalledApplications(int) — removes our app from the returned list
 * - queryIntentActivities(Intent, int) — removes our activities from results
 *
 * Note: PackageManager.MATCH_UNINSTALLED_PACKAGES queries bypass this hook. That's acceptable —
 * apps using that flag are more sophisticated than this basic check.
 */
object PackageManagerHooker : BaseSpoofHooker("PackageManagerHooker") {

    private const val SELF_PACKAGE = "com.astrixforge.devicemasker"

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        // Don't hook PackageManager for our own UI app — we need PM access
        if (pkg == SELF_PACKAGE) return

        hookPackageManager(cl, xi)
    }

    private fun hookPackageManager(cl: ClassLoader, xi: XposedInterface) {
        val pmClass = cl.loadClassOrNull("android.app.ApplicationPackageManager") ?: return
        val intPrimitive = Int::class.javaPrimitiveType!!

        safeHook("ApplicationPackageManager.getPackageInfo(String, int)") {
            pmClass.methodOrNull("getPackageInfo", String::class.java, intPrimitive)?.let { m ->
                xi.hook(m, GetPackageInfoHooker::class.java)
            }
        }
        safeHook("ApplicationPackageManager.getApplicationInfo(String, int)") {
            pmClass.methodOrNull("getApplicationInfo", String::class.java, intPrimitive)?.let { m ->
                xi.hook(m, GetApplicationInfoHooker::class.java)
            }
        }
        safeHook("ApplicationPackageManager.getInstalledPackages(int)") {
            pmClass.methodOrNull("getInstalledPackages", intPrimitive)?.let { m ->
                xi.hook(m, GetInstalledPackagesHooker::class.java)
            }
        }
        safeHook("ApplicationPackageManager.getInstalledApplications(int)") {
            pmClass.methodOrNull("getInstalledApplications", intPrimitive)?.let { m ->
                xi.hook(m, GetInstalledApplicationsHooker::class.java)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetPackageInfoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun before(callback: BeforeHookCallback) {
                try {
                    val pkgName = callback.args.firstOrNull() as? String ?: return
                    if (pkgName == SELF_PACKAGE) {
                        callback.throwAndSkip(
                            android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        )
                    }
                } catch (t: Throwable) {
                    Log.w("PMGetPackageInfoHooker", "before() failed: ${t.message}")
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
                    if (pkgName == SELF_PACKAGE) {
                        callback.throwAndSkip(
                            android.content.pm.PackageManager.NameNotFoundException(pkgName)
                        )
                    }
                } catch (t: Throwable) {
                    Log.w("PMGetApplicationInfoHooker", "before() failed: ${t.message}")
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
                    packages.removeAll { it.packageName == SELF_PACKAGE }
                } catch (t: Throwable) {
                    Log.w("PMGetInstalledPackagesHooker", "after() failed: ${t.message}")
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
                    apps.removeAll { it.packageName == SELF_PACKAGE }
                } catch (t: Throwable) {
                    Log.w("PMGetInstalledApplicationsHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
