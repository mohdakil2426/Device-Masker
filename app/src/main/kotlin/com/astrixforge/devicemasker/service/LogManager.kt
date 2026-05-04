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
import com.astrixforge.devicemasker.service.diagnostics.RootAccessManager
import com.astrixforge.devicemasker.service.diagnostics.RootCaptureStore
import com.astrixforge.devicemasker.service.diagnostics.RootLogCollector
import com.astrixforge.devicemasker.service.diagnostics.SupportBundleBuilder
import com.astrixforge.devicemasker.service.diagnostics.SupportBundleMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Log manager for rootless structured export.
 *
 * The app cannot read global logcat without privileged access. Exports combine app-owned persistent
 * logs with the current diagnostics service buffer when that service is reachable.
 */
object LogManager : ILogManager {

    private const val LOG_FILE_PREFIX = "devicemasker_support_"
    private const val LOG_FILE_EXTENSION = ".zip"

    override suspend fun exportLogsToUri(
        context: Context,
        uri: Uri,
        mode: SupportBundleMode,
    ): LogExportResult =
        withContext(Dispatchers.IO) {
            try {
                val bundle = buildSupportBundle(context, mode)

                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    LogExportResult.Error("Failed to open file for writing")
                } else {
                    outputStream.use { output ->
                        bundle.inputStream().use { input -> input.copyTo(output) }
                    }

                    LogExportResult.Success(
                        uri.lastPathSegment ?: bundle.name,
                        bundle.length().toInt(),
                    )
                }
            } catch (e: Exception) {
                LogExportResult.Error("Export failed: ${e.message}")
            }
        }

    override suspend fun createShareableLogFile(
        context: Context,
        mode: SupportBundleMode,
    ): ShareableLogResult =
        withContext(Dispatchers.IO) {
            try {
                if (mode != SupportBundleMode.ROOT_MAXIMUM && !hasAnyLogs(context)) {
                    return@withContext ShareableLogResult.NoLogs
                }

                val logsDir = File(context.cacheDir, "logs")
                if (!logsDir.exists()) {
                    logsDir.mkdirs()
                }

                val fileName = generateLogFileName()
                val bundle = buildSupportBundle(context, mode, logsDir)
                val logFile = File(logsDir, fileName)
                if (bundle.name != logFile.name) {
                    bundle.copyTo(logFile, overwrite = true)
                }

                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        logFile,
                    )

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
        val rootArtifactsDir =
            if (mode == SupportBundleMode.ROOT_MAXIMUM) {
                RootCaptureStore.prepareExportArtifacts(context, outputDir).also { rootDir ->
                    if (RootAccessManager.hasGrantedRoot()) {
                        RootLogCollector().collect(File(rootDir, "export_snapshot"), null)
                    } else {
                        RootCaptureStore.writeManifest(
                            dir = rootDir,
                            trigger = "export",
                            status = "ROOT_UNAVAILABLE",
                            message =
                                "Root access is not currently granted; export used captured root artifacts only.",
                        )
                    }
                }
            } else {
                null
            }
        val rootAvailable = RootAccessManager.hasGrantedRoot()
        val snapshots =
            DiagnosticSnapshotBuilder(
                    metadata =
                        DiagnosticSnapshotMetadata(
                            appVersion =
                                context.packageManager
                                    .getPackageInfo(context.packageName, 0)
                                    .versionName ?: "unknown",
                            buildType = com.astrixforge.devicemasker.BuildConfig.BUILD_TYPE,
                            androidSdk = Build.VERSION.SDK_INT,
                            androidRelease = Build.VERSION.RELEASE ?: "unknown",
                            device = "${Build.MANUFACTURER} ${Build.MODEL}",
                            rootAvailable = rootAvailable,
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
                appEvents =
                    appEvents.map { event ->
                        DiagnosticJson.encodeToString(DiagnosticEvent.serializer(), event)
                    },
                xposedEvents =
                    if (serviceClient.isConnected) serviceLogs
                    else listOf("""{"message":"Not available from service"}"""),
                serviceEvents = serviceLogs,
                snapshots = snapshots,
                rootArtifactsDir = rootArtifactsDir,
            )
            .build(outputDir, mode, RedactionMode.REDACTED)
    }

    suspend fun getLogCount(): Int =
        withContext(Dispatchers.IO) {
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
        }

    private suspend fun hasAnyLogs(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            val serviceClient = DeviceMaskerApp.serviceClient
            if (!serviceClient.isConnected) {
                serviceClient.connect()
            }
            AppLogStore.from(context).readEntries().isNotEmpty() ||
                (serviceClient.isConnected && serviceClient.getLogs(1).isNotEmpty())
        }

    override fun generateLogFileName(): String {
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
