package com.astrixforge.devicemasker.service

import kotlinx.coroutines.flow.StateFlow

interface IServiceClient {
    val connectionState: StateFlow<ServiceClient.ConnectionState>
    val isConnected: Boolean

    suspend fun connect(): Boolean

    suspend fun getHookedPackages(): List<String>

    suspend fun getLogs(maxCount: Int = 100): List<String>
}
