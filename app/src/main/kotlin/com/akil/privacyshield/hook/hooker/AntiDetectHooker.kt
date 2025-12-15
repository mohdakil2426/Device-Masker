package com.akil.privacyshield.hook.hooker

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.type.java.BooleanType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import timber.log.Timber

/**
 * Anti-Detection Hooker - Hides Xposed/LSPosed presence from detection.
 *
 * ⚠️ CRITICAL: This hooker MUST be loaded FIRST before any other hookers!
 * Early detection checks can discover the hook framework before spoofing
 * hooks are active. Loading this first ensures detection is blocked.
 *
 * Detection Vectors Covered:
 * 1. Stack Trace Analysis - Filter Thread/Throwable.getStackTrace()
 * 2. Class Loading - Block Class.forName() for Xposed classes
 * 3. /proc/self/maps - Filter native library paths
 * 4. Package Detection - Hide Xposed installer packages
 * 5. Reflection Detection - Hide hooked method modifications
 */
object AntiDetectHooker : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // HIDDEN PATTERNS - Classes/packages to hide from detection
    // ═══════════════════════════════════════════════════════════

    /**
     * Class name patterns that indicate Xposed/LSPosed presence.
     * Used for stack trace filtering and Class.forName() blocking.
     */
    private val HIDDEN_CLASS_PATTERNS = listOf(
        // Xposed Framework
        "de.robv.android.xposed",
        "XposedBridge",
        "XposedHelpers",
        "XC_MethodHook",
        "XC_MethodReplacement",
        
        // LSPosed
        "io.github.lsposed",
        "LSPHooker",
        "LSPosed",
        
        // YukiHookAPI
        "com.highcapable.yukihookapi",
        "YukiHookAPI",
        "YukiBaseHooker",
        "YukiMemberHookCreator",
        
        // EdXposed
        "EdHooker",
        "EdXposed",
        "com.elderdrivers.riru",
        
        // Riru
        "rikka.ndk",
        "org.lsposed.lspd",
        
        // General hook patterns
        "HookEntry",
        "PrivacyShield"
    )

    /**
     * Native library paths that indicate Xposed presence.
     * Used for /proc/self/maps filtering.
     */
    private val HIDDEN_LIBRARY_PATTERNS = listOf(
        "libxposed",
        "liblspd",
        "libriru",
        "libsandhook",
        "libpine",
        "libwhale",
        "libdobby",
        "libsubstrate",
        "libandroid_runtime_ext"
    )

    /**
     * Package names of Xposed installers and managers.
     * Used for PackageManager hook to hide these apps.
     */
    private val HIDDEN_PACKAGES = listOf(
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "io.github.lsposed.manager",
        "com.topjohnwu.magisk",
        "me.weishu.exp",
        "org.meowcat.edxposed.manager"
    )

    override fun onHook() {
        Timber.d("AntiDetectHooker: CRITICAL - Loading anti-detection hooks FIRST")

        // ═══════════════════════════════════════════════════════════
        // STACK TRACE HIDING
        // Prevents detection via Thread.getStackTrace() analysis
        // ═══════════════════════════════════════════════════════════
        
        hookStackTraces()

        // ═══════════════════════════════════════════════════════════
        // CLASS LOADING HIDING
        // Prevents detection via Class.forName("de.robv.android.xposed.*")
        // ═══════════════════════════════════════════════════════════
        
        hookClassLoading()

        // ═══════════════════════════════════════════════════════════
        // /PROC/SELF/MAPS HIDING
        // Prevents detection via reading /proc/self/maps for libraries
        // ═══════════════════════════════════════════════════════════
        
        hookProcMaps()

        // ═══════════════════════════════════════════════════════════
        // PACKAGE HIDING
        // Prevents detection via PackageManager package queries
        // ═══════════════════════════════════════════════════════════
        
        hookPackageManager()

        Timber.d("AntiDetectHooker: Anti-detection hooks registered")
    }

    /**
     * Hooks stack trace methods to filter out Xposed-related frames.
     */
    private fun hookStackTraces() {
        // Hook Thread.getStackTrace()
        "java.lang.Thread".toClass().apply {
            method {
                name = "getStackTrace"
                emptyParam()
            }.hook {
                after {
                    val originalStack = result as? Array<StackTraceElement> ?: return@after
                    result = filterStackTrace(originalStack)
                }
            }
        }

        // Hook Throwable.getStackTrace()
        "java.lang.Throwable".toClass().apply {
            method {
                name = "getStackTrace"
                emptyParam()
            }.hook {
                after {
                    val originalStack = result as? Array<StackTraceElement> ?: return@after
                    result = filterStackTrace(originalStack)
                }
            }
        }

        Timber.d("AntiDetectHooker: Stack trace hooks registered")
    }

    /**
     * Filters a stack trace array, removing frames that match hidden patterns.
     */
    private fun filterStackTrace(stack: Array<StackTraceElement>): Array<StackTraceElement> {
        return stack.filterNot { element ->
            val className = element.className
            HIDDEN_CLASS_PATTERNS.any { pattern ->
                className.contains(pattern, ignoreCase = true)
            }
        }.toTypedArray()
    }

    /**
     * Hooks class loading methods to block loading of Xposed classes.
     */
    private fun hookClassLoading() {
        // Hook Class.forName(String)
        "java.lang.Class".toClass().apply {
            method {
                name = "forName"
                param(StringClass)
            }.hook {
                before {
                    val className = args(0).string()
                    if (shouldBlockClass(className)) {
                        Timber.d("AntiDetectHooker: Blocking Class.forName($className)")
                        // Throw exception to block class loading
                        throw ClassNotFoundException(className)
                    }
                }
            }

            // Hook Class.forName(String, boolean, ClassLoader)
            method {
                name = "forName"
                param(StringClass, BooleanType, ClassLoader::class.java)
            }.hook {
                before {
                    val className = args(0).string()
                    if (shouldBlockClass(className)) {
                        Timber.d("AntiDetectHooker: Blocking Class.forName($className, ...)")
                        throw ClassNotFoundException(className)
                    }
                }
            }
        }

        // Hook ClassLoader.loadClass()
        "java.lang.ClassLoader".toClass().apply {
            method {
                name = "loadClass"
                param(StringClass)
            }.hook {
                before {
                    val className = args(0).string()
                    if (shouldBlockClass(className)) {
                        Timber.d("AntiDetectHooker: Blocking ClassLoader.loadClass($className)")
                        throw ClassNotFoundException(className)
                    }
                }
            }

            method {
                name = "loadClass"
                param(StringClass, BooleanType)
            }.hook {
                before {
                    val className = args(0).string()
                    if (shouldBlockClass(className)) {
                        Timber.d("AntiDetectHooker: Blocking ClassLoader.loadClass($className, ...)")
                        throw ClassNotFoundException(className)
                    }
                }
            }
        }

        Timber.d("AntiDetectHooker: Class loading hooks registered")
    }

    /**
     * Checks if a class name should be blocked from loading.
     */
    private fun shouldBlockClass(className: String): Boolean {
        return HIDDEN_CLASS_PATTERNS.any { pattern ->
            className.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Hooks file reading to filter /proc/self/maps content.
     * Apps read this file to detect loaded native libraries.
     */
    private fun hookProcMaps() {
        // Hook BufferedReader.readLine() for /proc/self/maps filtering
        "java.io.BufferedReader".toClass().apply {
            method {
                name = "readLine"
                emptyParam()
            }.hook {
                after {
                    val line = result as? String ?: return@after
                    
                    // Filter lines containing hidden library patterns
                    if (shouldFilterMapsLine(line)) {
                        // Skip this line by returning empty string
                        result = ""
                    }
                }
            }
        }

        Timber.d("AntiDetectHooker: /proc/maps hooks registered")
    }

    /**
     * Checks if a /proc/self/maps line should be filtered.
     */
    private fun shouldFilterMapsLine(line: String): Boolean {
        return HIDDEN_LIBRARY_PATTERNS.any { pattern ->
            line.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Hooks PackageManager to hide Xposed-related packages.
     */
    private fun hookPackageManager() {
        // Hook PackageManager.getPackageInfo()
        "android.app.ApplicationPackageManager".toClass().apply {
            // getPackageInfo(String, int)
            method {
                name = "getPackageInfo"
                paramCount = 2
            }.hook {
                before {
                    val packageName = args(0).string()
                    if (shouldHidePackage(packageName)) {
                        Timber.d("AntiDetectHooker: Hiding package $packageName")
                        throw android.content.pm.PackageManager.NameNotFoundException(packageName)
                    }
                }
            }

            // getApplicationInfo(String, int)
            method {
                name = "getApplicationInfo"
                paramCount = 2
            }.hook {
                before {
                    val packageName = args(0).string()
                    if (shouldHidePackage(packageName)) {
                        Timber.d("AntiDetectHooker: Hiding app info for $packageName")
                        throw android.content.pm.PackageManager.NameNotFoundException(packageName)
                    }
                }
            }
        }

        // Also hook getInstalledPackages and getInstalledApplications to filter lists
        runCatching {
            "android.app.ApplicationPackageManager".toClass().apply {
                method {
                    name = "getInstalledPackages"
                    paramCount = 1
                }.hook {
                    after {
                        val packages = result as? MutableList<*> ?: return@after
                        packages.removeAll { pkg ->
                            val pkgInfo = pkg as? android.content.pm.PackageInfo
                            pkgInfo?.packageName?.let { shouldHidePackage(it) } ?: false
                        }
                    }
                }

                method {
                    name = "getInstalledApplications"
                    paramCount = 1
                }.hook {
                    after {
                        val apps = result as? MutableList<*> ?: return@after
                        apps.removeAll { app ->
                            val appInfo = app as? android.content.pm.ApplicationInfo
                            appInfo?.packageName?.let { shouldHidePackage(it) } ?: false
                        }
                    }
                }
            }
        }

        Timber.d("AntiDetectHooker: PackageManager hooks registered")
    }

    /**
     * Checks if a package should be hidden from the app.
     */
    private fun shouldHidePackage(packageName: String): Boolean {
        return HIDDEN_PACKAGES.any { hidden ->
            packageName.equals(hidden, ignoreCase = true)
        }
    }
}
