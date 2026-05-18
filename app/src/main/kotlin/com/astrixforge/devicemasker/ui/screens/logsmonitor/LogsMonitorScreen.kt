package com.astrixforge.devicemasker.ui.screens.logsmonitor

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.service.logmonitor.LogCaptureStatus
import com.astrixforge.devicemasker.service.logmonitor.LogMonitorLevel
import com.astrixforge.devicemasker.service.logmonitor.LogMonitorRow
import com.astrixforge.devicemasker.service.logmonitor.LogMonitorSource
import com.astrixforge.devicemasker.ui.navigation.logsMonitorViewModelFactory

@Composable
fun LogsMonitorScreen(
    application: Application,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LogsMonitorViewModel = viewModel(factory = logsMonitorViewModelFactory(application)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LogsMonitorContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onStart = viewModel::startCapture,
        onStop = viewModel::stopCapture,
        onClear = viewModel::clear,
        onSourceSelect = viewModel::setSource,
        onMinLevelSelect = viewModel::setMinLevel,
        onQueryChange = viewModel::setQuery,
        modifier = modifier,
    )
}

@Composable
internal fun LogsMonitorContent(
    state: LogsMonitorState,
    onNavigateBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onSourceSelect: (LogMonitorSource) -> Unit,
    onMinLevelSelect: (LogMonitorLevel) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            LogsMonitorHeader(
                status = state.status,
                onNavigateBack = onNavigateBack,
                onStart = onStart,
                onStop = onStop,
                onClear = onClear,
            )
        }
        item(key = "filters") {
            LogsMonitorFilters(
                state = state,
                onSourceSelect = onSourceSelect,
                onMinLevelSelect = onMinLevelSelect,
                onQueryChange = onQueryChange,
            )
        }
        if (state.visibleRows.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.logs_monitor_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.visibleRows, key = { it.id }) { row -> LogRow(row) }
        }
    }
}

@Composable
private fun LogsMonitorHeader(
    status: LogCaptureStatus,
    onNavigateBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        LogsMonitorTitleRow(status = status, onNavigateBack = onNavigateBack)
        LogCaptureActions(status = status, onStart = onStart, onStop = onStop, onClear = onClear)
    }
}

@Composable
private fun LogsMonitorTitleRow(status: LogCaptureStatus, onNavigateBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            Text(
                text = stringResource(R.string.logs_monitor_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.logs_monitor_status, status.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LogCaptureActions(
    status: LogCaptureStatus,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    val isRunning = status == LogCaptureStatus.RUNNING
    val captureLabel =
        stringResource(if (isRunning) R.string.logs_monitor_stop else R.string.logs_monitor_start)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = if (isRunning) onStop else onStart,
            modifier = Modifier.weight(1f),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                contentDescription = captureLabel,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text = captureLabel)
        }
        OutlinedButton(
            onClick = onClear,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.logs_monitor_clear),
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(text = stringResource(R.string.logs_monitor_clear))
        }
    }
}

@Composable
private fun LogsMonitorFilters(
    state: LogsMonitorState,
    onSourceSelect: (LogMonitorSource) -> Unit,
    onMinLevelSelect: (LogMonitorLevel) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.logs_monitor_source_filter),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LogMonitorSource.entries.forEach { source ->
                FilterChip(
                    selected = state.filter.source == source,
                    onClick = { onSourceSelect(source) },
                    label = { Text(source.displayLabel()) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.logs_monitor_level_filter),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LogMonitorLevel.entries.forEach { level ->
                FilterChip(
                    selected = state.filter.minLevel == level,
                    onClick = { onMinLevelSelect(level) },
                    label = { Text(level.displayLabel()) },
                )
            }
        }
        OutlinedTextField(
            value = state.filter.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.logs_monitor_search)) },
        )
    }
}

@Composable
private fun LogRow(row: LogMonitorRow) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${row.level.displayLabel()} ${row.source.displayLabel()} ${row.tag}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = row.message,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun LogMonitorSource.displayLabel(): String =
    when (this) {
        LogMonitorSource.ALL -> "All"
        LogMonitorSource.APP -> "App"
        LogMonitorSource.XPOSED -> "Xposed"
        LogMonitorSource.LSPOSED -> "LSPosed"
        LogMonitorSource.CRASH -> "Crash"
        LogMonitorSource.LOGCAT -> "Logcat"
    }

private fun LogMonitorLevel.displayLabel(): String =
    when (this) {
        LogMonitorLevel.VERBOSE -> "Verbose"
        LogMonitorLevel.DEBUG -> "Debug"
        LogMonitorLevel.INFO -> "Info"
        LogMonitorLevel.WARN -> "Warn"
        LogMonitorLevel.ERROR -> "Error"
        LogMonitorLevel.FATAL -> "Fatal"
    }
