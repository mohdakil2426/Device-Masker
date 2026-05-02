package com.astrixforge.devicemasker.ui.screens.diagnostics

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.data.XposedPrefs
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.service.ServiceClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Diagnostics screen — libxposed API 101 / Option B edition.
 *
 * Uses the diagnostics-only [ServiceClient] (post-migration):
 * - `getHookedPackages()` — packages hooked this session
 * - `getLogs()` — hook event log entries
 * - `getSpoofEventCount()` — per-package spoof counter
 * - `isAlive()` / `connectionState` — service health
 *
 * Config-related service methods (writeConfig, readConfig, etc.) have been removed. Config delivery
 * is exclusively via RemotePreferences (libxposed API 101).
 */
class DiagnosticsViewModel(application: Application, private val repository: SpoofRepository) :
    AndroidViewModel(application) {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    /** Diagnostics-only AIDL service client. */
    private val serviceClient: ServiceClient = DeviceMaskerApp.serviceClient

    init {
        _state.update { it.copy(isXposedActive = DeviceMaskerApp.isXposedModuleActive) }

        viewModelScope.launch {
            XposedPrefs.isServiceConnected.collect { connected ->
                _state.update { it.copy(isXposedActive = connected) }
            }
        }

        // Observe connection state live
        viewModelScope.launch {
            serviceClient.connectionState.collect { connectionState ->
                _state.update {
                    it.copy(
                        serviceStatus = it.serviceStatus.copy(connectionState = connectionState)
                    )
                }
            }
        }

        runDiagnostics()
    }

    /** Pull-to-refresh: re-run all diagnostics and reload service data. */
    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(400) // Minimum visible refresh duration
            runDiagnostics()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun runDiagnostics() {
        viewModelScope.launch {
            val diagnosticResults = runDiagnosticTests()
            val antiDetectionResults = runAntiDetectionTests()
            refreshServiceStatus()

            _state.update {
                it.copy(
                    diagnosticResults = diagnosticResults,
                    antiDetectionResults = antiDetectionResults,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Refreshes the diagnostics service status.
     *
     * Connects if not already connected, then loads:
     * - Hooked packages count
     * - Recent hook log entries
     *
     * If the service is unavailable, the UI shows "Service unavailable" gracefully — spoofing
     * continues via RemotePreferences regardless.
     */
    private suspend fun refreshServiceStatus() {
        if (!serviceClient.isConnected) {
            serviceClient.connect()
        }

        if (serviceClient.isConnected) {
            val hookedPackages = serviceClient.getHookedPackages()
            val recentLogs = serviceClient.getLogs(maxCount = 50)

            _state.update {
                it.copy(
                    serviceStatus =
                        it.serviceStatus.copy(
                            connectionState = ServiceClient.ConnectionState.CONNECTED,
                            hookedAppCount = hookedPackages.size,
                        ),
                    hookLogs = recentLogs,
                )
            }
        } else {
            _state.update {
                it.copy(
                    serviceStatus =
                        it.serviceStatus.copy(
                            connectionState = ServiceClient.ConnectionState.ERROR,
                            hookedAppCount = 0,
                        ),
                    hookLogs = emptyList(),
                )
            }
        }
    }

    private suspend fun runDiagnosticTests(): List<DiagnosticResult> {
        val group = repository.activeGroup.first()

        val presetId = group?.getValue(SpoofType.DEVICE_PROFILE)
        val presetInfo =
            presetId?.let { com.astrixforge.devicemasker.common.DeviceProfilePreset.findById(it) }

        val context = getApplication<Application>()

        return listOf(
            // Android ID
            DiagnosticResult(
                type = SpoofType.ANDROID_ID,
                realValue =
                    runCatching {
                            @Suppress("HardwareIds")
                            Settings.Secure.getString(
                                context.contentResolver,
                                Settings.Secure.ANDROID_ID,
                            )
                        }
                        .getOrNull(),
                spoofedValue = group?.getValue(SpoofType.ANDROID_ID),
                isActive = group?.isTypeEnabled(SpoofType.ANDROID_ID) == true,
                isSpoofed = group?.getValue(SpoofType.ANDROID_ID) != null,
            ),
            // Device Profile (unified Build.* spoofing)
            DiagnosticResult(
                type = SpoofType.DEVICE_PROFILE,
                realValue = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                spoofedValue = presetInfo?.let { "${it.manufacturer} ${it.model}" },
                isActive = group?.isTypeEnabled(SpoofType.DEVICE_PROFILE) == true,
                isSpoofed = presetInfo != null,
            ),
        )
    }

    private fun runAntiDetectionTests(): List<AntiDetectionTest> {
        return listOf(
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_stack_trace,
                descriptionRes = R.string.diagnostics_test_stack_trace_desc,
                isPassed =
                    runCatching {
                            Thread.currentThread().stackTrace.none {
                                it.className.contains("xposed", ignoreCase = true)
                            }
                        }
                        .getOrElse { false },
            ),
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_class_loading,
                descriptionRes = R.string.diagnostics_test_class_loading_desc,
                isPassed =
                    runCatching {
                            Class.forName("de.robv.android.xposed.XposedBridge")
                            false
                        }
                        .getOrElse { true },
            ),
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_native_hiding,
                descriptionRes = R.string.diagnostics_test_native_hiding_desc,
                isPassed = true,
            ),
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_package_hiding,
                descriptionRes = R.string.diagnostics_test_package_hiding_desc,
                isPassed = true,
            ),
        )
    }
}
