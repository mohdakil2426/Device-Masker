package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.ExceptionMode
import java.lang.reflect.Method

/**
 * Package Manager Hooker — new in libxposed API 101 migration (Gap 4.10).
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

        safeHook("ApplicationPackageManager.getPackageInfo(String, flags)") {
            pmClass.packageLookupMethods("getPackageInfo").forEach { m ->
                xi.hook(m).setExceptionMode(ExceptionMode.PASSTHROUGH).intercept { chain ->
                    val pkgName = chain.args.firstOrNull() as? String
                    if (pkgName == SELF_PACKAGE) {
                        throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                    }
                    chain.proceed()
                }
                xi.deoptimize(m)
            }
        }
        safeHook("ApplicationPackageManager.getApplicationInfo(String, flags)") {
            pmClass.packageLookupMethods("getApplicationInfo").forEach { m ->
                xi.hook(m).setExceptionMode(ExceptionMode.PASSTHROUGH).intercept { chain ->
                    val pkgName = chain.args.firstOrNull() as? String
                    if (pkgName == SELF_PACKAGE) {
                        throw android.content.pm.PackageManager.NameNotFoundException(pkgName)
                    }
                    chain.proceed()
                }
                xi.deoptimize(m)
            }
        }
        safeHook("ApplicationPackageManager.getInstalledPackages(flags)") {
            pmClass.singleFlagMethods("getInstalledPackages").forEach { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    @Suppress("UNCHECKED_CAST")
                    val packages = result as? List<PackageInfo> ?: return@intercept result
                    packages.filterNot { it.packageName == SELF_PACKAGE }
                }
                xi.deoptimize(m)
            }
        }
        safeHook("ApplicationPackageManager.getInstalledApplications(flags)") {
            pmClass.singleFlagMethods("getInstalledApplications").forEach { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    @Suppress("UNCHECKED_CAST")
                    val apps = result as? List<ApplicationInfo> ?: return@intercept result
                    apps.filterNot { it.packageName == SELF_PACKAGE }
                }
                xi.deoptimize(m)
            }
        }
        safeHook("ApplicationPackageManager.queryIntentActivities(Intent, flags)") {
            pmClass.intentQueryMethods("queryIntentActivities").forEach { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    @Suppress("UNCHECKED_CAST")
                    val activities = result as? List<ResolveInfo> ?: return@intercept result
                    activities.filterNot { it.activityInfo?.packageName == SELF_PACKAGE }
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun Class<*>.packageLookupMethods(name: String): List<Method> =
        declaredMethods
            .filter {
                it.name == name &&
                    it.parameterCount == 2 &&
                    it.parameterTypes.firstOrNull() == String::class.java
            }
            .onEach { it.isAccessible = true }

    private fun Class<*>.singleFlagMethods(name: String): List<Method> =
        declaredMethods
            .filter { it.name == name && it.parameterCount == 1 }
            .onEach { it.isAccessible = true }

    private fun Class<*>.intentQueryMethods(name: String): List<Method> =
        declaredMethods
            .filter {
                it.name == name &&
                    it.parameterCount == 2 &&
                    it.parameterTypes.firstOrNull() == android.content.Intent::class.java
            }
            .onEach { it.isAccessible = true }
}
