package com.astrixforge.devicemasker.service.diagnostics

import java.io.File

class RootLogCollector(private val rootShell: RootShell = RootShell()) {
    fun collect(outputDir: File, targetPackage: String?): List<RootCommandResult> {
        outputDir.mkdirs()
        val target = targetPackage?.takeIf(::isValidPackageName)
        val targetPattern = target?.let { "|$it" }.orEmpty()
        val results = buildList {
            add(
                collectFile(
                    outputDir,
                    "logcat_main_system_crash.txt",
                    "logcat -d -v threadtime -b main,system,crash,events",
                )
            )
            add(
                collectFile(
                    outputDir,
                    "logcat_filtered_devicemasker_lsposed.txt",
                    "logcat -d -v threadtime | grep -i -E 'DeviceMasker|LSPosed|lspd|AndroidRuntime|FATAL EXCEPTION|ANR|com.mantle.verify$targetPattern'",
                )
            )
            add(collectFile(outputDir, "anr/list.txt", "ls /data/anr"))
            add(collectFile(outputDir, "anr/anr_traces.txt", "cat /data/anr/anr_*"))
            add(collectFile(outputDir, "tombstones/list.txt", "ls /data/tombstones"))
            add(
                collectFile(
                    outputDir,
                    "tombstones/tombstones.txt",
                    "cat /data/tombstones/tombstone_*",
                )
            )
            add(
                collectFile(
                    outputDir,
                    "dumpsys_package_module.txt",
                    "dumpsys package com.astrixforge.devicemasker",
                )
            )
            target?.let {
                add(collectFile(outputDir, "dumpsys_package_target.txt", "dumpsys package $it"))
            }
            add(
                collectFile(
                    outputDir,
                    "dumpsys_activity_processes.txt",
                    "dumpsys activity processes",
                )
            )
            add(collectFile(outputDir, "getprop_redacted.txt", "getprop"))
        }
        outputDir
            .resolve("command_manifest.jsonl")
            .writeText(results.joinToString("\n") { it.toJsonLine() }, Charsets.UTF_8)
        return results
    }

    private fun collectFile(
        outputDir: File,
        relativePath: String,
        command: String,
    ): RootCommandResult {
        val commandDir = File(outputDir, ".commands/${relativePath.replace('/', '_')}")
        val result = rootShell.run(RootCommand(command), commandDir)
        val destination = File(outputDir, relativePath)
        destination.parentFile?.mkdirs()
        destination.writeText(result.stdoutPath?.readText().orEmpty(), Charsets.UTF_8)
        return result
    }

    private fun isValidPackageName(value: String): Boolean =
        value.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))

    private fun RootCommandResult.toJsonLine(): String =
        """{"command":"${command.jsonEscape()}","status":"$status","exitCode":${exitCode ?: "null"},"timedOut":$timedOut,"durationMillis":$durationMillis,"rootAvailable":$rootAvailable,"stderrSummary":"${stderrSummary.jsonEscape()}"}"""

    private fun String.jsonEscape(): String = replace("\\", "\\\\").replace("\"", "\\\"")
}
