package com.astrixforge.devicemasker.ui.screens.home

import app.cash.turbine.test
import com.astrixforge.devicemasker.MainDispatcherRule
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.data.XposedScopeState
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.testing.FakeAppScopeRepository
import com.astrixforge.devicemasker.testing.FakeSpoofRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `module toggle updates state`() = runTest {
        val repository = FakeSpoofRepository()
        val viewModel = HomeViewModel(repository)

        viewModel.state.test {
            assertFalse(awaitItem().isModuleEnabled)

            viewModel.setModuleEnabled(true)
            assertTrue(awaitItem().isModuleEnabled)
        }
    }

    @Test
    fun `group selection updates selected group and counts`() = runTest {
        val group = SpoofGroup.createNew(name = "TestGroup", isDefault = true)
        val repository =
            FakeSpoofRepository(
                initialGroups = listOf(group),
                initialAppConfigs =
                    mapOf(
                        "com.example.app" to
                            AppConfig(
                                packageName = "com.example.app",
                                groupId = group.id,
                                isEnabled = true,
                            )
                    ),
            )
        val viewModel = HomeViewModel(repository)

        // Initial state
        assertEquals("TestGroup", viewModel.state.value.selectedGroup?.name)
        assertEquals(1, viewModel.state.value.enabledAppsCount)

        val group2 = SpoofGroup.createNew(name = "Other")
        repository.setGroups(listOf(group, group2))

        viewModel.selectGroup(group2.id)
        advanceUntilIdle()

        assertEquals("Other", viewModel.state.value.selectedGroup?.name)
        assertEquals(0, viewModel.state.value.enabledAppsCount)
    }

    @Test
    fun `xposed connection updates state`() = runTest {
        val xposedFlow = MutableStateFlow(false)
        val repository = FakeSpoofRepository()
        val viewModel = HomeViewModel(repository, isXposedActiveFlow = xposedFlow)

        viewModel.state.test {
            assertFalse(awaitItem().isXposedActive)

            xposedFlow.value = true
            assertTrue(awaitItem().isXposedActive)
        }
    }

    @Test
    fun `enabledAppsCount reflects repository`() = runTest {
        val group = SpoofGroup.createNew(name = "G1", isDefault = true)
        val repository =
            FakeSpoofRepository(
                initialGroups = listOf(group),
                initialAppConfigs =
                    mapOf(
                        "a.b.c" to AppConfig(packageName = "a.b.c", groupId = group.id),
                        "d.e.f" to AppConfig(packageName = "d.e.f", groupId = group.id),
                    ),
            )
        val viewModel = HomeViewModel(repository)

        viewModel.state.test { assertEquals(2, awaitItem().enabledAppsCount) }
    }

    @Test
    fun `loads installed apps so scoped apps appear without opening apps tab first`() = runTest {
        val appScopeRepository =
            FakeAppScopeRepository(
                appsLoadedFromSystem =
                    listOf(InstalledApp("com.scoped.app", "Scoped App", isSystemApp = false))
            )
        val repository = FakeSpoofRepository(appScopeRepository = appScopeRepository)
        val scopeState =
            MutableStateFlow<XposedScopeState>(XposedScopeState.Connected(setOf("com.scoped.app")))

        val viewModel = HomeViewModel(repository, xposedScopeStateFlow = scopeState)
        advanceUntilIdle()

        assertEquals(1, appScopeRepository.loadAppsCalls)
        assertEquals(false, appScopeRepository.lastForceRefresh)
        assertEquals(listOf("Scoped App"), viewModel.state.value.scopedApps.map { it.label })
    }

    @Test
    fun `refresh scoped apps force refreshes installed app cache`() = runTest {
        val appScopeRepository = FakeAppScopeRepository()
        val repository = FakeSpoofRepository(appScopeRepository = appScopeRepository)
        val viewModel =
            HomeViewModel(
                repository = repository,
                xposedScopeStateFlow = MutableStateFlow(XposedScopeState.Disconnected),
            )
        advanceUntilIdle()

        viewModel.refreshScopedApps()
        advanceUntilIdle()

        assertEquals(2, appScopeRepository.loadAppsCalls)
        assertEquals(true, appScopeRepository.lastForceRefresh)
    }
}
