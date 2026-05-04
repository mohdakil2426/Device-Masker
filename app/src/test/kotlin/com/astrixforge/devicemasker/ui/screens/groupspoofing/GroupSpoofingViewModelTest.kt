package com.astrixforge.devicemasker.ui.screens.groupspoofing

import app.cash.turbine.test
import com.astrixforge.devicemasker.MainDispatcherRule
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.testing.FakeSpoofRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GroupSpoofingViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `tab switching updates selected tab`() = runTest {
        val group = SpoofGroup.createNew(name = "G1")
        val repository = FakeSpoofRepository(initialGroups = listOf(group))
        val viewModel = GroupSpoofingViewModel(repository, group.id)

        viewModel.state.test {
            assertEquals(0, awaitItem().selectedTab)

            viewModel.setSelectedTab(2)
            assertEquals(2, awaitItem().selectedTab)
        }
    }

    @Test
    fun `value regeneration updates group`() = runTest {
        val group = SpoofGroup.createNew(name = "G1")
        val repository = FakeSpoofRepository(initialGroups = listOf(group))
        val viewModel = GroupSpoofingViewModel(repository, group.id)

        viewModel.state.test {
            val before = awaitItem()
            assertEquals(null, before.group?.getValue(SpoofType.ANDROID_ID))

            viewModel.regenerateValue(SpoofType.ANDROID_ID)
            val after = awaitItem()
            assertTrue(after.group?.getValue(SpoofType.ANDROID_ID)?.startsWith("fake-") == true)
        }
    }

    @Test
    fun `carrier update updates group values`() = runTest {
        val group = SpoofGroup.createNew(name = "G1")
        val repository = FakeSpoofRepository(initialGroups = listOf(group))
        val viewModel = GroupSpoofingViewModel(repository, group.id)

        viewModel.state.test {
            awaitItem()

            val carrier = Carrier.nextSecureRandom()
            viewModel.updateCarrier(carrier)
            val after = awaitItem()
            assertEquals(carrier.name, after.group?.getValue(SpoofType.CARRIER_NAME))
            assertEquals(carrier.mccMnc, after.group?.getValue(SpoofType.CARRIER_MCC_MNC))
        }
    }

    @Test
    fun `app assignment updates group`() = runTest {
        val group = SpoofGroup.createNew(name = "G1")
        val repository = FakeSpoofRepository(initialGroups = listOf(group))
        val viewModel = GroupSpoofingViewModel(repository, group.id)

        viewModel.state.test {
            val before = awaitItem()
            assertEquals(0, before.group?.assignedAppCount())

            viewModel.addAppToGroup("com.example.app")
            val after = awaitItem()
            assertEquals(1, after.group?.assignedAppCount())
        }
    }
}
