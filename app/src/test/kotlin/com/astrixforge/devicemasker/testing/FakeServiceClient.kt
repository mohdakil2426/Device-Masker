package com.astrixforge.devicemasker.testing

import com.astrixforge.devicemasker.service.IServiceClient
import com.astrixforge.devicemasker.service.ServiceClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fake [IServiceClient] for diagnostics testing. */
class FakeServiceClient(
    initialConnected: Boolean = false,
    private val hookedPackagesResult: List<String> = emptyList(),
    private val logsResult: List<String> = emptyList(),
) : IServiceClient {

    private val _connectionState =
        MutableStateFlow(
            if (initialConnected) ServiceClient.ConnectionState.CONNECTED
            else ServiceClient.ConnectionState.DISCONNECTED
        )
    override val connectionState: StateFlow<ServiceClient.ConnectionState> =
        _connectionState.asStateFlow()

    override val isConnected: Boolean
        get() = _connectionState.value == ServiceClient.ConnectionState.CONNECTED

    override suspend fun connect(): Boolean {
        _connectionState.value = ServiceClient.ConnectionState.CONNECTED
        return true
    }

    override suspend fun getHookedPackages(): List<String> = hookedPackagesResult

    override suspend fun getLogs(maxCount: Int): List<String> = logsResult.take(maxCount)

    fun setConnected(connected: Boolean) {
        _connectionState.value =
            if (connected) ServiceClient.ConnectionState.CONNECTED
            else ServiceClient.ConnectionState.DISCONNECTED
    }
}
