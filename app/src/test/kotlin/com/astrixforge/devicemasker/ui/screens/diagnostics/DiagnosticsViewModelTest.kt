package com.astrixforge.devicemasker.ui.screens.diagnostics

import app.cash.turbine.test
import com.astrixforge.devicemasker.MainDispatcherRule
import com.astrixforge.devicemasker.service.ServiceClient
import com.astrixforge.devicemasker.testing.FakeServiceClient
import com.astrixforge.devicemasker.testing.FakeSpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiagnosticsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        repository: FakeSpoofRepository = FakeSpoofRepository(),
        serviceClient: FakeServiceClient = FakeServiceClient(),
        isXposedActiveFlow: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ): DiagnosticsViewModel {
        val app = RuntimeEnvironment.getApplication()
        return DiagnosticsViewModel(app, repository, serviceClient, isXposedActiveFlow)
    }

    @Test
    fun `service connection reflected in state`() = runTest {
        val serviceClient = FakeServiceClient(initialConnected = true)
        val viewModel = createViewModel(serviceClient = serviceClient)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.serviceStatus.isConnected)
            assertEquals(
                ServiceClient.ConnectionState.CONNECTED,
                state.serviceStatus.connectionState,
            )
        }
    }

    @Test
    fun `refresh toggles isRefreshing`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            val initial = awaitItem()
            assertFalse(initial.isRefreshing)

            viewModel.refresh()
            val refreshing = awaitItem()
            assertTrue(refreshing.isRefreshing)

            val done = awaitItem()
            assertFalse(done.isRefreshing)
        }
    }

    @Test
    fun `anti-detection tests are populated`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            // antiDetectionResults are populated in init via runDiagnostics
            assertTrue(state.antiDetectionResults.isNotEmpty())
            // stack trace test should pass in test environment
            val stackTraceTest =
                state.antiDetectionResults.find {
                    it.nameRes == com.astrixforge.devicemasker.R.string.diagnostics_test_stack_trace
                }
            assertTrue(stackTraceTest?.isPassed == true)
        }
    }

    @Test
    fun `xposed active flow updates state`() = runTest {
        val xposedFlow = MutableStateFlow(false)
        val viewModel = createViewModel(isXposedActiveFlow = xposedFlow)

        viewModel.state.test {
            assertFalse(awaitItem().isXposedActive)

            xposedFlow.value = true
            assertTrue(awaitItem().isXposedActive)
        }
    }
}
