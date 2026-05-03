package com.astrixforge.devicemasker.service.diagnostics

import com.astrixforge.devicemasker.common.diagnostics.RedactionMode
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportBundleBuilderTest {
    @Test
    fun `root maximum zip contains expected support artifacts`() {
        val rootDir = createTempDirectory("root-artifacts").toFile()
        rootDir.resolve("logcat_main_system_crash.txt").writeText("imei=490154203237518")
        val outputDir = createTempDirectory("bundle").toFile()
        val bundle =
            SupportBundleBuilder(
                    appEvents = listOf("""{"message":"app"}"""),
                    xposedEvents = listOf("""{"message":"xposed"}"""),
                    serviceEvents = listOf("""{"message":"service"}"""),
                    snapshots =
                        mapOf(
                            "config_snapshot_redacted.json" to """{"imei":"[REDACTED_IMEI]"}""",
                            "remote_prefs_snapshot_redacted.json" to "{}",
                            "scope_snapshot.json" to "[]",
                            "hook_health.json" to "{}",
                            "summary.json" to "{}",
                        ),
                    rootArtifactsDir = rootDir,
                )
                .build(outputDir, SupportBundleMode.ROOT_MAXIMUM, RedactionMode.REDACTED)

        ZipFile(bundle).use { zip ->
            listOf(
                    "manifest.json",
                    "README_REPRO.md",
                    "app/app_events.jsonl",
                    "xposed/xposed_events.jsonl",
                    "diagnostics/service_events.jsonl",
                    "config/config_snapshot_redacted.json",
                    "config/remote_prefs_snapshot_redacted.json",
                    "scope/scope_snapshot.json",
                    "root/logcat_main_system_crash.txt",
                )
                .forEach { assertTrue("Missing $it", zip.getEntry(it) != null) }

            val combined =
                zip.entries().asSequence().joinToString("\n") { entry ->
                    zip.getInputStream(entry).bufferedReader().readText()
                }
            assertFalse(combined.contains("490154203237518"))
            assertTrue(combined.contains("[REDACTED_IMEI]"))
        }
    }
}
