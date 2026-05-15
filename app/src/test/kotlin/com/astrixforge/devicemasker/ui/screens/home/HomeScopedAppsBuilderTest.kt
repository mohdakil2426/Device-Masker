package com.astrixforge.devicemasker.ui.screens.home

import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.data.XposedScopeState
import com.astrixforge.devicemasker.data.models.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScopedAppsBuilderTest {

    @Test
    fun buildHomeScopedApps_usesLsposedScopeNotConfiguredApps() {
        val group = SpoofGroup.createNew("Work")
        val apps =
            buildHomeScopedApps(
                scopeState = XposedScopeState.Connected(setOf("com.scope.only")),
                installedApps =
                    listOf(
                        InstalledApp("com.scope.only", "Scoped App", isSystemApp = false),
                        InstalledApp("com.config.only", "Configured App", isSystemApp = false),
                    ),
                appConfigs =
                    mapOf(
                        "com.config.only" to
                            AppConfig(packageName = "com.config.only", groupId = group.id)
                    ),
                groups = listOf(group),
            )

        assertEquals(listOf("com.scope.only"), apps.map { it.packageName })
        assertEquals(HomeScopedAppStatus.NotConfigured, apps.single().status)
        assertTrue(apps.single().isGloballyEnabled)
    }

    @Test
    fun buildHomeScopedApps_excludesDefaultLsposedPackages() {
        val apps =
            buildHomeScopedApps(
                scopeState = XposedScopeState.Connected(setOf("android", "system", "com.app")),
                installedApps = listOf(InstalledApp("com.app", "User App", isSystemApp = false)),
                appConfigs = emptyMap(),
                groups = emptyList(),
            )

        assertEquals(listOf("com.app"), apps.map { it.packageName })
    }

    @Test
    fun buildHomeScopedApps_marksDisabledWhenAppOrGroupDisabled() {
        val disabledGroup = SpoofGroup.createNew("Disabled").copy(isEnabled = false)
        val enabledGroup = SpoofGroup.createNew("Enabled")

        val apps =
            buildHomeScopedApps(
                scopeState =
                    XposedScopeState.Connected(
                        setOf("com.app.disabled", "com.group.disabled", "com.enabled")
                    ),
                installedApps =
                    listOf(
                        InstalledApp("com.app.disabled", "App Disabled", isSystemApp = false),
                        InstalledApp("com.group.disabled", "Group Disabled", isSystemApp = false),
                        InstalledApp("com.enabled", "Enabled", isSystemApp = false),
                    ),
                appConfigs =
                    mapOf(
                        "com.app.disabled" to
                            AppConfig(
                                packageName = "com.app.disabled",
                                groupId = enabledGroup.id,
                                isEnabled = false,
                            ),
                        "com.group.disabled" to
                            AppConfig(
                                packageName = "com.group.disabled",
                                groupId = disabledGroup.id,
                            ),
                        "com.enabled" to
                            AppConfig(packageName = "com.enabled", groupId = enabledGroup.id),
                    ),
                groups = listOf(disabledGroup, enabledGroup),
            )

        val statusByPackage = apps.associate { it.packageName to it.status }
        assertEquals(HomeScopedAppStatus.DisabledByApp, statusByPackage["com.app.disabled"])
        assertEquals(HomeScopedAppStatus.DisabledByGroup, statusByPackage["com.group.disabled"])
        assertEquals(HomeScopedAppStatus.Enabled, statusByPackage["com.enabled"])
    }

    @Test
    fun buildHomeScopedApps_sortsByLabelAndKeepsDisabledAppsInAlphabeticalPlace() {
        val group = SpoofGroup.createNew("Enabled")

        val apps =
            buildHomeScopedApps(
                scopeState =
                    XposedScopeState.Connected(setOf("com.zebra", "com.alpha", "com.beta")),
                installedApps =
                    listOf(
                        InstalledApp("com.zebra", "Zebra", isSystemApp = false),
                        InstalledApp("com.alpha", "Alpha", isSystemApp = false),
                        InstalledApp("com.beta", "Beta", isSystemApp = false),
                    ),
                appConfigs =
                    mapOf(
                        "com.zebra" to AppConfig(packageName = "com.zebra", groupId = group.id),
                        "com.alpha" to
                            AppConfig(
                                packageName = "com.alpha",
                                groupId = group.id,
                                isEnabled = false,
                            ),
                        "com.beta" to AppConfig(packageName = "com.beta", groupId = group.id),
                    ),
                groups = listOf(group),
            )

        assertEquals(listOf("Alpha", "Beta", "Zebra"), apps.map { it.label })
        assertEquals(HomeScopedAppStatus.DisabledByApp, apps.first().status)
    }

    @Test
    fun homeScopedAppCardAlpha_mutesDisabledAppsOnly() {
        assertEquals(1f, homeScopedAppCardAlpha(isGloballyEnabled = true))
        assertEquals(0.62f, homeScopedAppCardAlpha(isGloballyEnabled = false))
    }

    @Test
    fun buildHomeScopedApps_keepsStandaloneHomeDisabledStateWithoutGroupAssignment() {
        val apps =
            buildHomeScopedApps(
                scopeState = XposedScopeState.Connected(setOf("com.disabled")),
                installedApps =
                    listOf(InstalledApp("com.disabled", "Disabled App", isSystemApp = false)),
                appConfigs =
                    mapOf(
                        "com.disabled" to
                            AppConfig(
                                packageName = "com.disabled",
                                groupId = null,
                                isEnabled = false,
                            )
                    ),
                groups = emptyList(),
            )

        assertEquals(HomeScopedAppStatus.DisabledByApp, apps.single().status)
        assertEquals(false, apps.single().isGloballyEnabled)
    }

    @Test
    fun buildHomeScopedApps_omitsScopePackagesMissingFromInstalledApps() {
        val apps =
            buildHomeScopedApps(
                scopeState = XposedScopeState.Connected(setOf("com.missing")),
                installedApps = emptyList(),
                appConfigs = emptyMap(),
                groups = emptyList(),
            )

        assertTrue(apps.isEmpty())
    }

    @Test
    fun buildHomeScopedApps_returnsEmptyWhenScopeUnavailable() {
        val apps =
            buildHomeScopedApps(
                scopeState = XposedScopeState.Disconnected,
                installedApps = listOf(InstalledApp("com.app", "App", isSystemApp = false)),
                appConfigs = emptyMap(),
                groups = emptyList(),
            )

        assertTrue(apps.isEmpty())
    }
}
