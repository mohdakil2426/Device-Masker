package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import com.astrixforge.devicemasker.xposed.DualLog
import io.github.libxposed.api.XposedInterface


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
                xi.hook(m).intercept { chain ->
                    val pkgName = chain.args.firstOrNull() as? String
                    if (pkgName == SELF_PACKAGE) {
                        throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                    }
                    chain.proceed()
                }
            }
        }
        safeHook("ApplicationPackageManager.getApplicationInfo(String, int)") {
            pmClass.methodOrNull("getApplicationInfo", String::class.java, intPrimitive)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val pkgName = chain.args.firstOrNull() as? String
                    if (pkgName == SELF_PACKAGE) {
                        throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                    }
                    chain.proceed()
                }
            }
        }
        safeHook("ApplicationPackageManager.getInstalledPackages(int)") {
            pmClass.methodOrNull("getInstalledPackages", intPrimitive)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    @Suppress("UNCHECKED_CAST")
                    val packages = result as? MutableList<PackageInfo>
                    packages?.removeAll { it.packageName == SELF_PACKAGE }
                    result
                }
            }
        }
        safeHook("ApplicationPackageManager.getInstalledApplications(int)") {
            pmClass.methodOrNull("getInstalledApplications", intPrimitive)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    @Suppress("UNCHECKED_CAST")
                    val apps = result as? MutableList<ApplicationInfo>
                    apps?.removeAll { it.packageName == SELF_PACKAGE }
                    result
                }
            }
        }
        safeHook("ApplicationPackageManager.queryIntentActivities(Intent, int)") {
            pmClass
                .methodOrNull(
                    "queryIntentActivities",
                    android.content.Intent::class.java,
                    intPrimitive,
                )
                ?.let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        @Suppress("UNCHECKED_CAST")
                        val activities = result as? MutableList<ResolveInfo>
                        activities?.removeAll { it.activityInfo?.packageName == SELF_PACKAGE }
                        result
                    }
                }
        }
    }


}
