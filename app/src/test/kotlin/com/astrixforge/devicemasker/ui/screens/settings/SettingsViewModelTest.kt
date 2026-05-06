package com.astrixforge.devicemasker.ui.screens.settings

import android.net.Uri
import app.cash.turbine.test
import com.astrixforge.devicemasker.MainDispatcherRule
import com.astrixforge.devicemasker.testing.FakeLogManager
import com.astrixforge.devicemasker.testing.FakeSettingsDataStore
import com.astrixforge.devicemasker.ui.theme.ThemeMode
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        settingsStore: FakeSettingsDataStore = FakeSettingsDataStore(),
        logManager: FakeLogManager = FakeLogManager(),
    ): SettingsViewModel {
        val app = RuntimeEnvironment.getApplication()
        return SettingsViewModel(
            app,
            settingsStore,
            logManager,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `theme change updates state`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().themeMode)

            viewModel.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().themeMode)
        }
    }

    @Test
    fun `export success updates state`() = runTest {
        val logManager = FakeLogManager()
        val viewModel = createViewModel(logManager = logManager)

        viewModel.exportLogsToUri(Uri.parse("content://test"))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isExportingLogs)
        assertTrue(viewModel.state.value.exportResult is ExportResult.Success)
    }

    @Test
    fun `export failure updates state`() = runTest {
        val logManager =
            FakeLogManager().apply {
                exportResult =
                    com.astrixforge.devicemasker.service.LogExportResult.Error("disk full")
            }
        val viewModel = createViewModel(logManager = logManager)

        viewModel.exportLogsToUri(Uri.parse("content://test"))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isExportingLogs)
        assertTrue(viewModel.state.value.exportResult is ExportResult.Error)
    }

    @Test
    fun `clear export result resets state`() = runTest {
        val logManager = FakeLogManager()
        val viewModel = createViewModel(logManager = logManager)

        viewModel.exportLogsToUri(Uri.parse("content://test"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.exportResult is ExportResult.Success)

        viewModel.clearExportResult()
        assertNull(viewModel.state.value.exportResult)
    }
}
