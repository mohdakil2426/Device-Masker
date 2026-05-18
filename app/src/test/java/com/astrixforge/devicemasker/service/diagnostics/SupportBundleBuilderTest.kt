package com.astrixforge.devicemasker.service.diagnostics

import com.astrixforge.devicemasker.common.diagnostics.RedactionMode
import java.util.zip.ZipFile
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportBundleBuilderTest {
    @Test
    fun `support bundle streams app and xposed events as jsonl`() {
        val outputDir = createTempDirectory("bundle").toFile()
        val appEvents = (1..1000).map { """{"message":"app-$it"}""" }
        val xposedEvents = (1..1000).map { """{"message":"xposed-$it"}""" }

        val bundle =
            SupportBundleBuilder(appEvents = appEvents, xposedEvents = xposedEvents)
                .build(outputDir, RedactionMode.UNREDACTED)

        ZipFile(bundle).use { zip ->
            val appText =
                zip.getInputStream(zip.getEntry("app/app_events.jsonl")).bufferedReader().readText()
            val xposedText =
                zip.getInputStream(zip.getEntry("xposed/xposed_events.jsonl"))
                    .bufferedReader()
                    .readText()
            assertTrue(appText.contains("""{"message":"app-1000"}"""))
            assertTrue(xposedText.contains("""{"message":"xposed-1000"}"""))
        }
    }

    @Test
    fun `root maximum zip contains expected support artifacts`() {
        val rootDir = createTempDirectory("root-artifacts").toFile()
        rootDir.resolve("logcat_main_system_crash.txt").writeText("imei=490154203237518")
        rootDir.resolve("command_manifest.jsonl").writeText("""{"status":"EXITED"}""")
        val outputDir = createTempDirectory("bundle").toFile()
        val bundle =
            SupportBundleBuilder(
                    appEvents = listOf("""{"message":"app"}"""),
                    xposedEvents = listOf("""{"message":"xposed"}"""),
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
                .build(outputDir, RedactionMode.REDACTED)

        ZipFile(bundle).use { zip ->
            listOf(
                    "manifest.json",
                    "README_REPRO.md",
                    "app/app_events.jsonl",
                    "xposed/xposed_events.jsonl",
                    "config/config_snapshot_redacted.json",
                    "config/remote_prefs_snapshot_redacted.json",
                    "scope/scope_snapshot.json",
                    "root/logcat_main_system_crash.txt",
                    "root/command_manifest.jsonl",
                )
                .forEach { assertTrue("Missing $it", zip.getEntry(it) != null) }

            assertTrue(
                zip.getInputStream(zip.getEntry("manifest.json"))
                    .bufferedReader()
                    .readText()
                    .contains(""""mode":"MAXIMUM"""")
            )

            val combined =
                zip.entries().asSequence().joinToString("\n") { entry ->
                    zip.getInputStream(entry).bufferedReader().readText()
                }
            assertFalse(combined.contains("490154203237518"))
            assertTrue(combined.contains("[REDACTED_IMEI]"))
        }
    }
}
