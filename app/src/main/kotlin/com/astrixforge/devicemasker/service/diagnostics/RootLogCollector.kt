package com.astrixforge.devicemasker.service.diagnostics

import java.io.File

class RootLogCollector(private val rootShell: RootShell = RootShell()) {
    fun collect(outputDir: File, targetPackage: String?): List<RootCommandResult> {
        outputDir.mkdirs()
        val target = targetPackage?.takeIf(::isValidPackageName)
        val evidencePattern = buildEvidencePattern(target)
        val results = collectAllFiles(outputDir, target, evidencePattern)
        writeManifest(outputDir, results)
        return results
    }

    private fun buildEvidencePattern(target: String?): String =
        buildList {
                add("DeviceMasker")
                add("LSPosed")
                add("lspd")
                add("XposedEntry")
                add("All hooks registered")
                add("Spoof event")
                add("AndroidRuntime")
                add("FATAL EXCEPTION")
                add("ANR")
                target?.let(::add)
            }
            .joinToString("|")

    private fun collectAllFiles(
        outputDir: File,
        target: String?,
        evidencePattern: String,
    ): List<RootCommandResult> = buildList {
        add(collectLogcatMain(outputDir))
        add(collectLogcatFiltered(outputDir, evidencePattern))
        add(collectFile(outputDir, "anr/list.txt", "ls /data/anr"))
        add(collectFile(outputDir, "anr/anr_traces.txt", "cat /data/anr/anr_*"))
        add(collectFile(outputDir, "tombstones/list.txt", "ls /data/tombstones"))
        add(collectTombstones(outputDir))
        add(collectDumpsysModule(outputDir))
        target?.let { add(collectDumpsysTarget(outputDir, it)) }
        add(collectDumpsysActivity(outputDir))
        add(collectFile(outputDir, "getprop_redacted.txt", "getprop"))
    }

    private fun collectLogcatMain(outputDir: File) =
        collectFile(
            outputDir,
            "logcat_main_system_crash.txt",
            "logcat -d -v threadtime -b main,system,crash,events",
        )

    private fun collectLogcatFiltered(outputDir: File, evidencePattern: String): RootCommandResult {
        val grepCommand = "logcat -d -v threadtime | grep -i -E '$evidencePattern'"
        return collectFile(outputDir, "logcat_filtered_devicemasker_lsposed.txt", grepCommand)
    }

    private fun collectTombstones(outputDir: File) =
        collectFile(outputDir, "tombstones/tombstones.txt", "cat /data/tombstones/tombstone_*")

    private fun collectDumpsysModule(outputDir: File) =
        collectFile(
            outputDir,
            "dumpsys_package_module.txt",
            "dumpsys package com.astrixforge.devicemasker",
        )

    private fun collectDumpsysTarget(outputDir: File, target: String) =
        collectFile(outputDir, "dumpsys_package_target.txt", "dumpsys package $target")

    private fun collectDumpsysActivity(outputDir: File) =
        collectFile(outputDir, "dumpsys_activity_processes.txt", "dumpsys activity processes")

    private fun writeManifest(outputDir: File, results: List<RootCommandResult>) {
        outputDir
            .resolve("command_manifest.jsonl")
            .writeText(results.joinToString("\n") { it.toJsonLine() }, Charsets.UTF_8)
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
