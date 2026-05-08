package com.astrixforge.devicemasker.data

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofType

internal data class AppSyncState(
    val packageName: String,
    val appEnabled: Boolean,
    val riskyHooksEnabled: Boolean,
    val classLookupHidingEnabled: Boolean,
    val spoofTypes: List<SpoofTypeSyncState>,
)

internal data class SpoofTypeSyncState(
    val type: SpoofType,
    val enabled: Boolean,
    val value: String?,
)

internal fun JsonConfig.syncStateFor(packageName: String): AppSyncState? {
    val group = getGroupForApp(packageName) ?: return null
    val configApp = getAppConfig(packageName)
    val appEnabled = isModuleEnabled && configApp?.isEnabled == true && group.isEnabled
    val riskyHooksEnabled = appEnabled && configApp.riskyHooksEnabled
    val classLookupHidingEnabled = riskyHooksEnabled && configApp.classLookupHidingEnabled

    return AppSyncState(
        packageName = packageName,
        appEnabled = appEnabled,
        riskyHooksEnabled = riskyHooksEnabled,
        classLookupHidingEnabled = classLookupHidingEnabled,
        spoofTypes =
            SpoofType.entries.map { type ->
                val typeEnabled = appEnabled && group.isTypeEnabled(type)
                val value =
                    if (typeEnabled) {
                        group.getValue(type)?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    }
                SpoofTypeSyncState(
                    type = type,
                    enabled = typeEnabled && value != null,
                    value = value,
                )
            },
    )
}

internal fun SharedPreferences.Editor.putAppSyncState(state: AppSyncState) {
    putBoolean(SharedPrefsKeys.getAppEnabledKey(state.packageName), state.appEnabled)
    putBoolean(SharedPrefsKeys.getRiskyHooksEnabledKey(state.packageName), state.riskyHooksEnabled)
    putBoolean(
        SharedPrefsKeys.getClassLookupHidingEnabledKey(state.packageName),
        state.classLookupHidingEnabled,
    )
    state.spoofTypes.forEach { putSpoofTypeState(state.packageName, it) }
}

internal fun SharedPreferences.Editor.putSpoofTypeState(
    packageName: String,
    state: SpoofTypeSyncState,
) {
    putBoolean(SharedPrefsKeys.getSpoofEnabledKey(packageName, state.type), state.enabled)
    val valueKey = SharedPrefsKeys.getSpoofValueKey(packageName, state.type)
    if (state.value != null) {
        putString(valueKey, state.value)
    } else {
        remove(valueKey)
    }
}

internal fun SharedPreferences.Editor.putAppDisabled(
    packageName: String
): SharedPreferences.Editor = putBoolean(SharedPrefsKeys.getAppEnabledKey(packageName), false)

internal fun keysForSyncedPackage(packageName: String): Set<String> = buildSet {
    add(SharedPrefsKeys.getAppEnabledKey(packageName))
    SpoofType.entries.forEach { type ->
        add(SharedPrefsKeys.getSpoofEnabledKey(packageName, type))
        add(SharedPrefsKeys.getSpoofValueKey(packageName, type))
    }
    add(SharedPrefsKeys.getRiskyHooksEnabledKey(packageName))
    add(SharedPrefsKeys.getClassLookupHidingEnabledKey(packageName))
    add(SharedPrefsKeys.getPersonaBlobKey(packageName))
    add(SharedPrefsKeys.getPersonaVersionKey(packageName))
}
