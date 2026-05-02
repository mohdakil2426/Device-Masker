package com.astrixforge.devicemasker.service

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppLogStoreTest {

    @get:Rule val temp = TemporaryFolder()

    @Test
    fun `app log store persists sanitized entries`() {
        val store = AppLogStore(File(temp.root, "structured.log"), maxEntries = 10)

        store.append(
            AppLogEntry(
                timestampMillis = 1_700_000_000_000,
                level = "I",
                source = "app",
                tag = "Config\tManager",
                message = "Loaded\nconfig",
            )
        )

        val entries = store.readEntries()

        assertEquals(1, entries.size)
        assertEquals("Config Manager", entries.single().tag)
        assertEquals("Loaded config", entries.single().message)
    }

    @Test
    fun `app log store trims oldest entries`() {
        val store = AppLogStore(File(temp.root, "structured.log"), maxEntries = 2)

        store.append(AppLogEntry(1, "D", "app", "One", "first"))
        store.append(AppLogEntry(2, "D", "app", "Two", "second"))
        store.append(AppLogEntry(3, "D", "app", "Three", "third"))

        val entries = store.readEntries()

        assertEquals(listOf("second", "third"), entries.map { it.message })
    }

    @Test
    fun `log file formatter emits clean minimal file with real entries`() {
        val content =
            LogFileFormatter.build(
                appEntries =
                    listOf(
                        AppLogEntry(
                            timestampMillis = 1_700_000_000_000,
                            level = "I",
                            source = "app",
                            tag = "ConfigManager",
                            message = "Config loaded",
                        )
                    ),
                serviceLogs = listOf("[13:40:04.441] I/DMService: Hooks registered for app"),
                diagnosticsStatus = "connected",
                exportedAtMillis = 1_700_000_100_000,
            )

        assertTrue(content.contains("Device Masker Logs"))
        assertTrue(content.contains("diagnostics=connected"))
        assertTrue(content.contains("app_entries=1"))
        assertTrue(content.contains("service_entries=1"))
        assertTrue(content.contains("ConfigManager Config loaded"))
        assertTrue(content.contains("[xposed]"))
        assertFalse(content.contains("════════"))
        assertFalse(content.contains("Possible reasons:"))
    }
}
