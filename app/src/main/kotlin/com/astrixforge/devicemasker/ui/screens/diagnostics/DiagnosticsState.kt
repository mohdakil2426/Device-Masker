package com.astrixforge.devicemasker.ui.screens.diagnostics

import androidx.compose.runtime.Immutable
import com.astrixforge.devicemasker.common.SpoofType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** UI state for the Diagnostics screen. */
@Immutable
data class DiagnosticsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isXposedActive: Boolean = false,
    val diagnosticResults: ImmutableList<DiagnosticResult> = persistentListOf(),
    val antiDetectionResults: ImmutableList<AntiDetectionTest> = persistentListOf(),
    val reproCaptureState: ReproCaptureState = ReproCaptureState.IDLE,
)

enum class ReproCaptureState {
    IDLE,
    CAPTURING,
    STOPPING,
    EXPORT_READY,
    ERROR,
}

/** Data class representing a diagnostic result. */
@Immutable
data class DiagnosticResult(
    val type: SpoofType,
    val realValue: String?,
    val spoofedValue: String?,
    val isActive: Boolean,
    val isSpoofed: Boolean,
) {
    val status: DiagnosticStatus
        get() =
            when {
                !isActive -> DiagnosticStatus.INACTIVE
                isSpoofed -> DiagnosticStatus.SUCCESS
                else -> DiagnosticStatus.WARNING
            }
}

enum class DiagnosticStatus {
    SUCCESS,
    WARNING,
    INACTIVE,
}

/** Anti-detection test result. */
@Immutable
data class AntiDetectionTest(val nameRes: Int, val descriptionRes: Int, val isPassed: Boolean)
