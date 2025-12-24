package com.astrixforge.devicemasker.ui.screens.settings

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the Settings screen.
 *
 * Manages theme and debug settings via SettingsDataStore.
 *
 * @param application Application for context access
 * @param settingsStore The SettingsDataStore for persistence
 */
class SettingsViewModel(
    application: Application,
    private val settingsStore: SettingsDataStore
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        // Collect theme mode
        viewModelScope.launch {
            settingsStore.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }

        // Collect AMOLED mode
        viewModelScope.launch {
            settingsStore.amoledMode.collect { enabled ->
                _state.update { it.copy(amoledMode = enabled) }
            }
        }

        // Collect dynamic colors
        viewModelScope.launch {
            settingsStore.dynamicColors.collect { enabled ->
                _state.update { it.copy(dynamicColors = enabled) }
            }
        }

        // Collect debug logging
        viewModelScope.launch {
            settingsStore.debugLogging.collect { enabled ->
                _state.update { it.copy(debugLogging = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAmoledMode(enabled)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDynamicColors(enabled)
        }
    }

    fun setDebugLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDebugLogging(enabled)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOG EXPORT FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Exports logs from the Xposed service to a file in Downloads folder.
     */
    fun exportLogs() {
        viewModelScope.launch {
            _state.update { it.copy(isExportingLogs = true, exportResult = null) }

            val result = withContext(Dispatchers.IO) {
                // Log collection via AIDL service is no longer available
                // Suggest using adb logcat instead
                ExportResult.Error(
                    "Log collection via AIDL removed. " +
                    "Use: adb logcat -s PrivacyShield DeviceHooker NetworkHooker"
                )
            }

            _state.update { it.copy(isExportingLogs = false, exportResult = result) }
        }
    }

    /**
     * Clears the export result from state.
     */
    fun clearExportResult() {
        _state.update { it.copy(exportResult = null) }
    }
}
