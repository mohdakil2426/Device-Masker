package com.astrixforge.devicemasker.service.diagnostics

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RootShellTest {
    @Test
    fun `su unavailable returns root unavailable`() {
        val shell = RootShell(FakeExecutor(available = false))

        val result = shell.run(RootCommand("id"), createTempDirectory("root").toFile())

        assertFalse(result.rootAvailable)
        assertEquals(RootCommandStatus.ROOT_UNAVAILABLE, result.status)
    }

    @Test
    fun `captures exit code stderr and capped stdout`() {
        val shell =
            RootShell(
                FakeExecutor(
                    stdout = "abcdef",
                    stderr = "warning",
                    exitCode = 7,
                )
            )
        val dir = createTempDirectory("root").toFile()

        val result = shell.run(RootCommand("id", maxOutputBytes = 3), dir)

        assertEquals(7, result.exitCode)
        assertEquals("abc", result.stdoutPath?.readText())
        assertEquals("warning", result.stderrPath?.readText())
        assertEquals(RootCommandStatus.EXITED, result.status)
    }

    @Test
    fun `timeout returns timeout result`() {
        val shell = RootShell(FakeExecutor(timedOut = true))

        val result = shell.run(RootCommand("logcat", timeoutMillis = 1), createTempDirectory("root").toFile())

        assertTrue(result.timedOut)
        assertEquals(RootCommandStatus.TIMED_OUT, result.status)
    }

    private class FakeExecutor(
        private val available: Boolean = true,
        private val stdout: String = "",
        private val stderr: String = "",
        private val exitCode: Int = 0,
        private val timedOut: Boolean = false,
    ) : RootCommandExecutor {
        override fun isRootAvailable(): Boolean = available

        override fun execute(command: String, timeoutMillis: Long): RootExecutionResult =
            RootExecutionResult(
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                timedOut = timedOut,
                durationMillis = timeoutMillis,
            )
    }
}
