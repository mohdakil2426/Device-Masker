package com.astrixforge.devicemasker.service.diagnostics

import com.topjohnwu.superuser.Shell
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class RootCommand(
    val command: String,
    val timeoutMillis: Long = 5_000,
    val maxOutputBytes: Int = 256 * 1024,
)

enum class RootCommandStatus {
    EXITED,
    TIMED_OUT,
    ROOT_UNAVAILABLE,
}

data class RootCommandResult(
    val command: String,
    val status: RootCommandStatus,
    val exitCode: Int?,
    val stdoutPath: File?,
    val stderrPath: File?,
    val stderrSummary: String,
    val timedOut: Boolean,
    val durationMillis: Long,
    val rootAvailable: Boolean,
)

data class RootExecutionResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean,
    val durationMillis: Long,
)

interface RootCommandExecutor {
    fun isRootAvailable(): Boolean

    fun execute(command: String, timeoutMillis: Long): RootExecutionResult
}

class RootShell(private val executor: RootCommandExecutor = LibsuCommandExecutor()) {
    fun run(command: RootCommand, outputDir: File): RootCommandResult {
        outputDir.mkdirs()
        if (!executor.isRootAvailable()) {
            val result =
                RootCommandResult(
                    command = command.command,
                    status = RootCommandStatus.ROOT_UNAVAILABLE,
                    exitCode = null,
                    stdoutPath = null,
                    stderrPath = null,
                    stderrSummary = "",
                    timedOut = false,
                    durationMillis = 0,
                    rootAvailable = false,
                )
            outputDir.resolve("manifest.json").writeText(result.toManifestJson(""), Charsets.UTF_8)
            return result
        }

        val execution = executor.execute(command.command, command.timeoutMillis)
        val stdoutFile = File(outputDir, "stdout.txt")
        val stderrFile = File(outputDir, "stderr.txt")
        stdoutFile.writeText(execution.stdout.limitBytes(command.maxOutputBytes), Charsets.UTF_8)
        stderrFile.writeText(execution.stderr, Charsets.UTF_8)

        val result =
            RootCommandResult(
                command = command.command,
                status =
                    if (execution.timedOut) RootCommandStatus.TIMED_OUT
                    else RootCommandStatus.EXITED,
                exitCode = execution.exitCode,
                stdoutPath = stdoutFile,
                stderrPath = stderrFile,
                stderrSummary = execution.stderr.take(500),
                timedOut = execution.timedOut,
                durationMillis = execution.durationMillis,
                rootAvailable = true,
            )
        outputDir
            .resolve("manifest.json")
            .writeText(result.toManifestJson(execution.stderr), Charsets.UTF_8)
        return result
    }

    private fun String.limitBytes(maxBytes: Int): String {
        val bytes = toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return this
        return String(bytes.copyOf(maxBytes), Charsets.UTF_8)
    }
}

class LibsuCommandExecutor : RootCommandExecutor {
    override fun isRootAvailable(): Boolean = RootAccessManager.hasGrantedRoot()

    override fun execute(command: String, timeoutMillis: Long): RootExecutionResult {
        val started = System.currentTimeMillis()
        val executor = Executors.newSingleThreadExecutor()
        val future =
            executor.submit<RootExecutionResult> {
                val stdout = mutableListOf<String>()
                val stderr = mutableListOf<String>()
                val result = Shell.cmd(command).to(stdout, stderr).exec()
                RootExecutionResult(
                    exitCode = result.code,
                    stdout = stdout.joinToString("\n"),
                    stderr = stderr.joinToString("\n"),
                    timedOut = false,
                    durationMillis = System.currentTimeMillis() - started,
                )
            }
        return try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            future.cancel(true)
            RootExecutionResult(
                exitCode = -1,
                stdout = "",
                stderr = "Timed out after ${timeoutMillis}ms",
                timedOut = true,
                durationMillis = System.currentTimeMillis() - started,
            )
        } finally {
            executor.shutdownNow()
        }
    }
}

private fun RootCommandResult.toManifestJson(stderr: String): String =
    """{"command":"${command.jsonEscape()}","status":"$status","exitCode":${exitCode ?: "null"},"timedOut":$timedOut,"durationMillis":$durationMillis,"rootAvailable":$rootAvailable,"stderrSummary":"${stderr.take(500).jsonEscape()}"}"""

private fun String.jsonEscape(): String = buildString {
    this@jsonEscape.forEach { ch ->
        when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(ch)
        }
    }
}
