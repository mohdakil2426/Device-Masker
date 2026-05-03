package com.astrixforge.devicemasker.service.diagnostics

import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertTrue
import org.junit.Test

class RootLogCollectorTest {
    @Test
    fun `collector writes expected maximum diagnostic artifacts`() {
        val executor = RecordingExecutor()
        val collector = RootLogCollector(RootShell(executor))
        val outputDir = createTempDirectory("collector").toFile()

        collector.collect(outputDir, targetPackage = "com.mantle.verify")

        listOf(
                "logcat_main_system_crash.txt",
                "logcat_filtered_devicemasker_lsposed.txt",
                "anr/list.txt",
                "anr/anr_traces.txt",
                "tombstones/list.txt",
                "tombstones/tombstones.txt",
                "dumpsys_package_module.txt",
                "dumpsys_package_target.txt",
                "dumpsys_activity_processes.txt",
                "getprop_redacted.txt",
            )
            .forEach { relativePath -> assertTrue(outputDir.resolve(relativePath).exists()) }

        assertTrue(executor.commands.any { it.contains("logcat -d -v threadtime -b main,system,crash,events") })
        assertTrue(executor.commands.any { it.contains("dumpsys package com.mantle.verify") })
    }

    private class RecordingExecutor : RootCommandExecutor {
        val commands = mutableListOf<String>()

        override fun isRootAvailable(): Boolean = true

        override fun execute(command: String, timeoutMillis: Long): RootExecutionResult {
            commands += command
            return RootExecutionResult(0, "output for $command", "", false, 1)
        }
    }
}
