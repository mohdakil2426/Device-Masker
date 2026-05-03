package com.astrixforge.devicemasker.ui.screens.diagnostics

import com.astrixforge.devicemasker.service.ServiceClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiagnosticsStateTest {
    @Test
    fun `disconnected diagnostics service does not claim zero hook evidence`() {
        val status = ServiceStatus(connectionState = ServiceClient.ConnectionState.DISCONNECTED)

        assertEquals(HookEvidenceState.UNAVAILABLE, status.hookEvidenceState)
        assertEquals(null, status.hookedAppCount)
        assertFalse(status.isConnected)
    }

    @Test
    fun `connected diagnostics service can report observed hook evidence`() {
        val status =
            ServiceStatus(
                connectionState = ServiceClient.ConnectionState.CONNECTED,
                hookedAppCount = 2,
            )

        assertEquals(HookEvidenceState.OBSERVED, status.hookEvidenceState)
        assertEquals(2, status.hookedAppCount)
    }
}
