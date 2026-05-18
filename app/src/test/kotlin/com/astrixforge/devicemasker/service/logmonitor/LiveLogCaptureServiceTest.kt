package com.astrixforge.devicemasker.service.logmonitor

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveLogCaptureServiceTest {
    @Test
    fun `live logcat command tails only current logs`() {
        assertEquals(
            listOf(
                "su",
                "-c",
                "now=$(date +%m-%d\\ %H:%M:%S.000); exec logcat -b all -v threadtime -T \"\$now\"",
            ),
            liveLogcatCommand(),
        )
    }
}
