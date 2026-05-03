package com.astrixforge.devicemasker.service

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticEvent
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticJson
import com.astrixforge.devicemasker.common.diagnostics.RedactionMode
import com.astrixforge.devicemasker.service.diagnostics.DiagnosticSnapshotBuilder
import com.astrixforge.devicemasker.service.diagnostics.DiagnosticSnapshotMetadata
import com.astrixforge.devicemasker.service.diagnostics.SupportBundleBuilder
import com.astrixforge.devicemasker.service.diagnostics.SupportBundleMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log manager for rootless structured export.
 *
 * The app cannot read global logcat without privileged access. Exports combine app-owned persistent
 * logs with the current diagnostics service buffer when that service is reachable.
 */
object LogManager {

    private const val LOG_FILE_PREFIX = "devicemasker_support_"
    private const val LOG_FILE_EXTENSION = ".zip"

    suspend fun exportLogsToUri(context: Context, uri: Uri): LogExportResult {
        return try {
            val bundle = buildSupportBundle(context, SupportBundleMode.BASIC)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bundle.inputStream().use { input -> input.copyTo(outputStream) }
            } ?: return LogExportResult.Error("Failed to open file for writing")

            LogExportResult.Success(uri.lastPathSegment ?: bundle.name, bundle.length().toInt())
        } catch (e: Exception) {
            LogExportResult.Error("Export failed: ${e.message}")
        }
    }

    suspend fun createShareableLogFile(context: Context): ShareableLogResult {
        return try {
            if (!hasAnyLogs(context)) {
                return ShareableLogResult.NoLogs
            }

            val logsDir = File(context.cacheDir, "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }

            val fileName = generateLogFileName()
            val bundle = buildSupportBundle(context, SupportBundleMode.BASIC, logsDir)
            val logFile = File(logsDir, fileName)
            if (bundle.name != logFile.name) {
                bundle.copyTo(logFile, overwrite = true)
            }

            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)

            ShareableLogResult.Success(uri, fileName, logFile.length().toInt())
        } catch (e: Exception) {
            ShareableLogResult.Error("Failed to create shareable log: ${e.message}")
        }
    }

    private suspend fun buildSupportBundle(
        context: Context,
        mode: SupportBundleMode,
        outputDir: File = File(context.cacheDir, "logs"),
    ): File {
        val appEvents = AppLogStore.from(context).readDiagnosticEvents()
        val serviceClient = DeviceMaskerApp.serviceClient
        if (!serviceClient.isConnected) {
            serviceClient.connect()
        }

        val serviceLogs =
            if (serviceClient.isConnected) {
                serviceClient.getLogs(500)
            } else {
                emptyList()
            }
        val snapshots =
            DiagnosticSnapshotBuilder(
                    metadata =
                        DiagnosticSnapshotMetadata(
                            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown",
                            buildType = com.astrixforge.devicemasker.BuildConfig.BUILD_TYPE,
                            androidSdk = Build.VERSION.SDK_INT,
                            androidRelease = Build.VERSION.RELEASE ?: "unknown",
                            device = "${Build.MANUFACTURER} ${Build.MODEL}",
                            rootAvailable = false,
                            xposedServiceConnected = serviceClient.isConnected,
                            moduleEnabled = DeviceMaskerApp.isXposedModuleActive,
                            targetPackage = null,
                            scopePackages = listOf("android", "system"),
                            droppedLogCount = 0,
                        ),
                    configJson = "{}",
                    remotePrefs = emptyMap(),
                    hookHealthJson = "{}",
                )
                .build(RedactionMode.REDACTED)

        return SupportBundleBuilder(
                appEvents = appEvents.map { event ->
                    DiagnosticJson.encodeToString(DiagnosticEvent.serializer(), event)
                },
                xposedEvents = emptyList(),
                serviceEvents = serviceLogs,
                snapshots = snapshots,
            )
            .build(outputDir, mode, RedactionMode.REDACTED)
    }

    suspend fun getLogCount(): Int =
        try {
            val serviceClient = DeviceMaskerApp.serviceClient
            if (!serviceClient.isConnected) {
                serviceClient.connect()
            }
            AppLogStore.from(DeviceMaskerApp.getInstance()).readEntries().size +
                if (serviceClient.isConnected) serviceClient.getLogs(500).size else 0
        } catch (_: Exception) {
            0
        }

    private suspend fun hasAnyLogs(context: Context): Boolean {
        val serviceClient = DeviceMaskerApp.serviceClient
        if (!serviceClient.isConnected) {
            serviceClient.connect()
        }
        return AppLogStore.from(context).readEntries().isNotEmpty() ||
            (serviceClient.isConnected && serviceClient.getLogs(1).isNotEmpty())
    }

    fun generateLogFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
    }
}

sealed class LogExportResult {
    data class Success(val filePath: String, val lineCount: Int) : LogExportResult()

    data class Error(val message: String) : LogExportResult()
}

sealed class ShareableLogResult {
    data class Success(val uri: Uri, val fileName: String, val lineCount: Int) :
        ShareableLogResult()

    data class Error(val message: String) : ShareableLogResult()

    data object NoLogs : ShareableLogResult()
}
