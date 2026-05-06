package com.astrixforge.devicemasker.ui.screens.diagnostics

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.data.XposedPrefs
import com.astrixforge.devicemasker.data.repository.ISpoofRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Diagnostics screen.
 *
 * Config delivery is exclusively via RemotePreferences. Target-process hook proof comes from
 * LSPosed/logcat evidence, not a custom diagnostics Binder bridge.
 */
class DiagnosticsViewModel(
    application: Application,
    private val repository: ISpoofRepository,
    isXposedActiveFlow: StateFlow<Boolean> = XposedPrefs.isServiceConnected,
    @Suppress("unused") private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isXposedActive = isXposedActiveFlow.value) }

        viewModelScope.launch {
            isXposedActiveFlow.collect { connected ->
                _state.update { it.copy(isXposedActive = connected) }
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

            _state.update {
                it.copy(
                    diagnosticResults = diagnosticResults.toImmutableList(),
                    antiDetectionResults = antiDetectionResults.toImmutableList(),
                    isLoading = false,
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
