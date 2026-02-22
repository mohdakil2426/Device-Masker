package com.astrixforge.devicemasker.ui.screens.diagnostics

import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.service.ServiceClient

/** UI state for the Diagnostics screen. */
data class DiagnosticsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isXposedActive: Boolean = false,
    val diagnosticResults: List<DiagnosticResult> = emptyList(),
    val antiDetectionResults: List<AntiDetectionTest> = emptyList(),
    val serviceStatus: ServiceStatus = ServiceStatus(),
)

/** Status information about the AIDL service running in system_server. */
data class ServiceStatus(
    val connectionState: ServiceClient.ConnectionState = ServiceClient.ConnectionState.DISCONNECTED,
    val version: String? = null,
    val uptimeMs: Long = 0L,
    val hookedAppCount: Int = 0,
    val totalFilterCount: Int = 0,
) {
    /** Checks if service is connected and responsive. */
    val isConnected: Boolean
        get() = connectionState == ServiceClient.ConnectionState.CONNECTED

    /** Formats uptime as human-readable string. */
    val uptimeFormatted: String
        get() {
            if (uptimeMs <= 0) return "--"
            val seconds = (uptimeMs / 1000) % 60
            val minutes = (uptimeMs / (1000 * 60)) % 60
            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
            val days = uptimeMs / (1000 * 60 * 60 * 24)

            return when {
                days > 0 -> "${days}d ${hours}h ${minutes}m"
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
}

/** Data class representing a diagnostic result. */
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
data class AntiDetectionTest(val nameRes: Int, val descriptionRes: Int, val isPassed: Boolean)

