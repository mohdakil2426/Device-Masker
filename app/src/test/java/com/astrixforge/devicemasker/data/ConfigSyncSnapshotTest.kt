package com.astrixforge.devicemasker.data

import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.DeviceIdentifier
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigSyncSnapshotTest {

    @Test
    fun `snapshot uses app config as canonical scope and honors disabled app`() {
        val group =
            SpoofGroup.createNew("Work")
                .copy(
                    id = "group-work",
                    identifiers =
                        mapOf(
                            SpoofType.IMEI to
                                DeviceIdentifier(
                                    type = SpoofType.IMEI,
                                    value = "490154203237518",
                                    isEnabled = true,
                                )
                        ),
                    assignedApps = setOf("com.legacy.stale"),
                )
        val config =
            JsonConfig(
                groups = mapOf(group.id to group),
                appConfigs =
                    mapOf(
                        "com.example.enabled" to
                            AppConfig("com.example.enabled", groupId = group.id, isEnabled = true),
                        "com.example.disabled" to
                            AppConfig("com.example.disabled", groupId = group.id, isEnabled = false),
                    ),
            )

        val snapshot = ConfigSync.buildSnapshot(config, previousEnabledApps = emptySet())

        assertEquals(setOf("com.example.enabled", "com.example.disabled"), snapshot.currentApps)
        assertTrue(
            snapshot.booleans.getValue(SharedPrefsKeys.getAppEnabledKey("com.example.enabled"))
        )
        assertFalse(
            snapshot.booleans.getValue(SharedPrefsKeys.getAppEnabledKey("com.example.disabled"))
        )
        assertEquals(
            "490154203237518",
            snapshot.strings[
                    SharedPrefsKeys.getSpoofValueKey("com.example.enabled", SpoofType.IMEI)],
        )
        assertNull(
            snapshot.strings[
                    SharedPrefsKeys.getSpoofValueKey("com.example.disabled", SpoofType.IMEI)]
        )
    }

    @Test
    fun `snapshot clears packages removed since the last sync`() {
        val group =
            SpoofGroup.createNew("Work")
                .copy(
                    id = "group-work",
                    identifiers =
                        mapOf(
                            SpoofType.IMEI to
                                DeviceIdentifier(
                                    type = SpoofType.IMEI,
                                    value = "490154203237518",
                                    isEnabled = true,
                                )
                        ),
                )
        val config =
            JsonConfig(
                groups = mapOf(group.id to group),
                appConfigs =
                    mapOf(
                        "com.example.current" to
                            AppConfig("com.example.current", groupId = group.id, isEnabled = true)
                    ),
            )

        val snapshot =
            ConfigSync.buildSnapshot(
                config,
                previousEnabledApps = setOf("com.example.current", "com.example.removed"),
            )

        assertTrue(snapshot.removedApps.contains("com.example.removed"))
        assertTrue(
            snapshot.removeKeys.contains(SharedPrefsKeys.getAppEnabledKey("com.example.removed"))
        )
        assertTrue(
            snapshot.removeKeys.contains(
                SharedPrefsKeys.getSpoofEnabledKey("com.example.removed", SpoofType.IMEI)
            )
        )
        assertTrue(
            snapshot.removeKeys.contains(
                SharedPrefsKeys.getSpoofValueKey("com.example.removed", SpoofType.IMEI)
            )
        )
    }
}
