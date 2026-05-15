package com.astrixforge.devicemasker.testing

import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.data.repository.IAppScopeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake [IAppScopeRepository] for ViewModel testing. */
class FakeAppScopeRepository(
    initialApps: List<InstalledApp> = emptyList(),
    private val appsLoadedFromSystem: List<InstalledApp> = initialApps,
) : IAppScopeRepository {

    private val _installedApps = MutableStateFlow(initialApps)
    override val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    var loadAppsCalls = 0
        private set

    var lastForceRefresh: Boolean? = null
        private set

    override suspend fun loadApps(forceRefresh: Boolean) {
        loadAppsCalls += 1
        lastForceRefresh = forceRefresh
        _isLoading.value = true
        _installedApps.value = appsLoadedFromSystem
        _isLoading.value = false
    }

    override suspend fun getInstalledApps(
        includeSystem: Boolean,
        refreshCache: Boolean,
    ): List<InstalledApp> {
        return if (includeSystem) {
            _installedApps.value
        } else {
            _installedApps.value.filter { !it.isSystemApp }
        }
    }

    override fun invalidateCache() {
        // no-op for fake
    }

    fun setApps(apps: List<InstalledApp>) {
        _installedApps.value = apps
    }
}
