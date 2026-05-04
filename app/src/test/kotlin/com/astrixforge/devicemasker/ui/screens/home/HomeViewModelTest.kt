package com.astrixforge.devicemasker.ui.screens.home

import app.cash.turbine.test
import com.astrixforge.devicemasker.MainDispatcherRule
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.testing.FakeSpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
        val group =
            SpoofGroup.createNew(name = "TestGroup", isDefault = true).addApp("com.example.app")
        val repository = FakeSpoofRepository(initialGroups = listOf(group))
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
        val group =
            SpoofGroup.createNew(name = "G1", isDefault = true).addApp("a.b.c").addApp("d.e.f")
        val repository = FakeSpoofRepository(initialGroups = listOf(group))
        val viewModel = HomeViewModel(repository)

        viewModel.state.test { assertEquals(2, awaitItem().enabledAppsCount) }
    }
}
