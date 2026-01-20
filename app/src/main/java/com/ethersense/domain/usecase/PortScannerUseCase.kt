package com.ethersense.domain.usecase

import com.ethersense.data.model.CommonPorts
import com.ethersense.data.model.PortScanProgress
import com.ethersense.data.model.PortScanResult
import com.ethersense.data.model.PortStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.UnknownHostException
import javax.inject.Inject

class PortScannerUseCase @Inject constructor() {

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 1000
        private const val CONCURRENT_SCANS = 50
    }

    fun scanCommonPorts(
        host: String,
        timeout: Int = DEFAULT_TIMEOUT_MS
    ): Flow<PortScanResult> = flow {
        val ports = CommonPorts.COMMON_PORTS.map { it.first }

        for (port in ports) {
            val result = scanPort(host, port, timeout)
            emit(result)
        }
    }.flowOn(Dispatchers.IO)

    fun scanRange(
        host: String,
        startPort: Int,
        endPort: Int,
        timeout: Int = DEFAULT_TIMEOUT_MS
    ): Flow<PortScanResult> = flow {
        val ports = (startPort..endPort).toList()

        ports.chunked(CONCURRENT_SCANS).forEach { chunk ->
            coroutineScope {
                val results = chunk.map { port ->
                    async {
                        scanPort(host, port, timeout)
                    }
                }.awaitAll()

                results.forEach { emit(it) }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun scanWithProgress(
        host: String,
        ports: List<Int>,
        timeout: Int = DEFAULT_TIMEOUT_MS,
        onProgress: suspend (PortScanProgress) -> Unit
    ): Flow<PortScanResult> = flow {
        var openCount = 0
        var closedCount = 0
        var filteredCount = 0
        var scannedCount = 0

        ports.chunked(CONCURRENT_SCANS).forEach { chunk ->
            coroutineScope {
                val results = chunk.map { port ->
                    async {
                        scanPort(host, port, timeout)
                    }
                }.awaitAll()

                results.forEach { result ->
                    emit(result)
                    scannedCount++

                    when (result.status) {
                        PortStatus.OPEN -> openCount++
                        PortStatus.CLOSED -> closedCount++
                        PortStatus.FILTERED -> filteredCount++
                    }

                    onProgress(
                        PortScanProgress(
                            currentPort = result.port,
                            totalPorts = ports.size,
                            openPorts = openCount,
                            closedPorts = closedCount,
                            filteredPorts = filteredCount
                        )
                    )
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun scanPort(
        host: String,
        port: Int,
        timeout: Int = DEFAULT_TIMEOUT_MS
    ): PortScanResult {
        val startTime = System.currentTimeMillis()

        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeout)

                val responseTime = System.currentTimeMillis() - startTime

                PortScanResult(
                    host = host,
                    port = port,
                    status = PortStatus.OPEN,
                    serviceName = CommonPorts.getServiceName(port),
                    responseTime = responseTime
                )
            }
        } catch (e: java.net.SocketTimeoutException) {
            PortScanResult(
                host = host,
                port = port,
                status = PortStatus.FILTERED,
                serviceName = CommonPorts.getServiceName(port)
            )
        } catch (e: java.net.ConnectException) {
            PortScanResult(
                host = host,
                port = port,
                status = PortStatus.CLOSED,
                serviceName = CommonPorts.getServiceName(port)
            )
        } catch (e: NoRouteToHostException) {
            // Host is unreachable - different from filtered
            PortScanResult(
                host = host,
                port = port,
                status = PortStatus.FILTERED,
                serviceName = CommonPorts.getServiceName(port)
            )
        } catch (e: UnknownHostException) {
            // Cannot resolve hostname - treat as closed since host doesn't exist
            PortScanResult(
                host = host,
                port = port,
                status = PortStatus.CLOSED,
                serviceName = CommonPorts.getServiceName(port)
            )
        } catch (e: SecurityException) {
            // Permission denied
            PortScanResult(
                host = host,
                port = port,
                status = PortStatus.FILTERED,
                serviceName = CommonPorts.getServiceName(port)
            )
        } catch (e: Exception) {
            // Other network errors - treat as filtered
            PortScanResult(
                host = host,
                port = port,
                status = PortStatus.FILTERED,
                serviceName = CommonPorts.getServiceName(port)
            )
        }
    }
}
