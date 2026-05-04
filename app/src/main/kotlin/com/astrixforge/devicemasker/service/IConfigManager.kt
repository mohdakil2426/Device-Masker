package com.astrixforge.devicemasker.service

import android.content.Context
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for configuration management to enable testability.
 *
 * Extracted from [ConfigManager] so that repositories and ViewModels can accept fakes in tests.
 */
interface IConfigManager {
    val config: StateFlow<JsonConfig>
    val isInitialized: StateFlow<Boolean>

    fun init(context: Context)

    fun saveConfig()

    fun syncCurrentConfig()

    fun isModuleEnabled(): Boolean

    fun setModuleEnabled(enabled: Boolean)

    fun getAllGroups(): List<SpoofGroup>

    fun getGroup(groupId: String): SpoofGroup?

    fun createGroup(name: String, copyFromGroupId: String? = null): SpoofGroup

    fun updateGroup(group: SpoofGroup)

    fun deleteGroup(groupId: String)

    fun getGroupForApp(packageName: String): SpoofGroup?

    fun setIdentifierValue(groupId: String, type: SpoofType, value: String?)

    fun setTypeEnabled(groupId: String, type: SpoofType, enabled: Boolean)

    fun regenerateAllValues(groupId: String)

    fun getPersonaSeed(group: SpoofGroup): String

    fun getPersonaVersion(group: SpoofGroup): Long

    fun refreshPersonaLifecycle(group: SpoofGroup): SpoofGroup

    fun getAppConfig(packageName: String): AppConfig?

    fun assignAppToGroup(packageName: String, groupId: String)

    fun unassignApp(packageName: String)

    fun setAppEnabled(packageName: String, enabled: Boolean)
}
