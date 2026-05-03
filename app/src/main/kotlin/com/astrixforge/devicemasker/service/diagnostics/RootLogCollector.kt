package com.astrixforge.devicemasker.service.diagnostics

import java.io.File

class RootLogCollector(private val rootShell: RootShell = RootShell()) {
    fun collect(outputDir: File, targetPackage: String?): List<RootCommandResult> {
        outputDir.mkdirs()
        val target = targetPackage.orEmpty()
        return listOf(
            collectFile(
                outputDir,
                "logcat_main_system_crash.txt",
                "logcat -d -v threadtime -b main,system,crash,events",
            ),
            collectFile(
                outputDir,
                "logcat_filtered_devicemasker_lsposed.txt",
                "logcat -d -v threadtime | grep -i -E 'DeviceMasker|LSPosed|lspd|AndroidRuntime|FATAL EXCEPTION|ANR|com.mantle.verify|$target'",
            ),
            collectFile(outputDir, "anr/list.txt", "ls /data/anr"),
            collectFile(outputDir, "anr/anr_traces.txt", "cat /data/anr/anr_*"),
            collectFile(outputDir, "tombstones/list.txt", "ls /data/tombstones"),
            collectFile(outputDir, "tombstones/tombstones.txt", "cat /data/tombstones/tombstone_*"),
            collectFile(outputDir, "dumpsys_package_module.txt", "dumpsys package com.astrixforge.devicemasker"),
            collectFile(outputDir, "dumpsys_package_target.txt", "dumpsys package $target"),
            collectFile(outputDir, "dumpsys_activity_processes.txt", "dumpsys activity processes"),
            collectFile(outputDir, "getprop_redacted.txt", "getprop"),
        )
    }

    private fun collectFile(outputDir: File, relativePath: String, command: String): RootCommandResult {
        val commandDir = File(outputDir, ".commands/${relativePath.replace('/', '_')}")
        val result = rootShell.run(RootCommand(command), commandDir)
        val destination = File(outputDir, relativePath)
        destination.parentFile?.mkdirs()
        destination.writeText(result.stdoutPath?.readText().orEmpty(), Charsets.UTF_8)
        return result
    }
}
