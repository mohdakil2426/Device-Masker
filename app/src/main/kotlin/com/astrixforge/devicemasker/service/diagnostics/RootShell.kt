package com.astrixforge.devicemasker.service.diagnostics

import java.io.File
import java.util.concurrent.TimeUnit

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

class RootShell(private val executor: RootCommandExecutor = SuCommandExecutor()) {
    fun run(command: RootCommand, outputDir: File): RootCommandResult {
        outputDir.mkdirs()
        if (!executor.isRootAvailable()) {
            return RootCommandResult(
                command = command.command,
                status = RootCommandStatus.ROOT_UNAVAILABLE,
                exitCode = null,
                stdoutPath = null,
                stderrPath = null,
                timedOut = false,
                durationMillis = 0,
                rootAvailable = false,
            )
        }

        val execution = executor.execute(command.command, command.timeoutMillis)
        val stdoutFile = File(outputDir, "stdout.txt")
        val stderrFile = File(outputDir, "stderr.txt")
        stdoutFile.writeText(execution.stdout.limitBytes(command.maxOutputBytes), Charsets.UTF_8)
        stderrFile.writeText(execution.stderr, Charsets.UTF_8)

        return RootCommandResult(
            command = command.command,
            status = if (execution.timedOut) RootCommandStatus.TIMED_OUT else RootCommandStatus.EXITED,
            exitCode = execution.exitCode,
            stdoutPath = stdoutFile,
            stderrPath = stderrFile,
            timedOut = execution.timedOut,
            durationMillis = execution.durationMillis,
            rootAvailable = true,
        )
    }

    private fun String.limitBytes(maxBytes: Int): String {
        val bytes = toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return this
        return String(bytes.copyOf(maxBytes), Charsets.UTF_8)
    }
}

class SuCommandExecutor : RootCommandExecutor {
    override fun isRootAvailable(): Boolean =
        runCatching {
                val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(false).start()
                process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0
            }
            .getOrDefault(false)

    override fun execute(command: String, timeoutMillis: Long): RootExecutionResult {
        val started = System.currentTimeMillis()
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(false).start()
        val completed = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
        if (!completed) {
            process.destroyForcibly()
        }
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        return RootExecutionResult(
            exitCode = if (completed) process.exitValue() else -1,
            stdout = stdout,
            stderr = stderr,
            timedOut = !completed,
            durationMillis = System.currentTimeMillis() - started,
        )
    }
}
