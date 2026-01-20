package com.ethersense.domain.usecase

import com.ethersense.data.model.PingProgress
import com.ethersense.data.model.PingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

class PingUseCase @Inject constructor() {

    operator fun invoke(host: String, count: Int = 4): Flow<PingProgress> = flow {
        val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", count.toString(), host))
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        try {
            var line: String?
            var sequenceNumber = 0

            while (reader.readLine().also { line = it } != null) {
                line?.let { parseLine ->
                    parsePingLine(parseLine, host)?.let { progress ->
                        sequenceNumber++
                        emit(progress.copy(sequenceNumber = sequenceNumber))
                    }
                }
            }
        } finally {
            process.waitFor()
            reader.close()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun executeAndGetResult(host: String, count: Int = 4): PingResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", count.toString(), host))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            val output: String
            val exitCode: Int
            try {
                output = reader.readText()
                exitCode = process.waitFor()
            } finally {
                reader.close()
            }

            if (exitCode == 0) {
                parsePingOutput(host, output)
            } else {
                PingResult(
                    host = host,
                    ipAddress = "",
                    rttMin = 0f,
                    rttAvg = 0f,
                    rttMax = 0f,
                    packetLoss = 100f,
                    packetsTransmitted = count,
                    packetsReceived = 0,
                    isSuccess = false,
                    errorMessage = "Host unreachable"
                )
            }
        } catch (e: Exception) {
            PingResult(
                host = host,
                ipAddress = "",
                rttMin = 0f,
                rttAvg = 0f,
                rttMax = 0f,
                packetLoss = 100f,
                packetsTransmitted = count,
                packetsReceived = 0,
                isSuccess = false,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    private fun parsePingLine(line: String, host: String): PingProgress? {
        // Parse lines like: "64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=12.3 ms"
        val pattern = Regex("""(\d+) bytes from ([^:]+): icmp_seq=(\d+) ttl=(\d+) time=([0-9.]+) ms""")
        val match = pattern.find(line)

        return match?.let {
            val (_, ipOrHost, seq, ttl, time) = it.destructured
            PingProgress(
                sequenceNumber = seq.toInt(),
                rtt = time.toFloat(),
                ttl = ttl.toInt(),
                host = ipOrHost
            )
        }
    }

    private fun parsePingOutput(host: String, output: String): PingResult {
        var ipAddress = ""
        var rttMin = 0f
        var rttAvg = 0f
        var rttMax = 0f
        var packetLoss = 0f
        var packetsTransmitted = 0
        var packetsReceived = 0

        // Parse IP address from first line like "PING google.com (142.250.196.110)"
        val ipPattern = Regex("""\(([0-9.]+)\)""")
        ipPattern.find(output)?.let {
            ipAddress = it.groupValues[1]
        }

        // Parse packet statistics like "4 packets transmitted, 4 received, 0% packet loss"
        val statsPattern = Regex("""(\d+) packets transmitted, (\d+) (?:packets )?received, (\d+)% packet loss""")
        statsPattern.find(output)?.let {
            packetsTransmitted = it.groupValues[1].toInt()
            packetsReceived = it.groupValues[2].toInt()
            packetLoss = it.groupValues[3].toFloat()
        }

        // Parse RTT statistics like "rtt min/avg/max/mdev = 11.234/12.456/13.678/0.789 ms"
        val rttPattern = Regex("""rtt min/avg/max/mdev = ([0-9.]+)/([0-9.]+)/([0-9.]+)/([0-9.]+) ms""")
        rttPattern.find(output)?.let {
            rttMin = it.groupValues[1].toFloat()
            rttAvg = it.groupValues[2].toFloat()
            rttMax = it.groupValues[3].toFloat()
        }

        // Alternative pattern for some Android versions
        if (rttMin == 0f) {
            val altRttPattern = Regex("""round-trip min/avg/max = ([0-9.]+)/([0-9.]+)/([0-9.]+) ms""")
            altRttPattern.find(output)?.let {
                rttMin = it.groupValues[1].toFloat()
                rttAvg = it.groupValues[2].toFloat()
                rttMax = it.groupValues[3].toFloat()
            }
        }

        return PingResult(
            host = host,
            ipAddress = ipAddress,
            rttMin = rttMin,
            rttAvg = rttAvg,
            rttMax = rttMax,
            packetLoss = packetLoss,
            packetsTransmitted = packetsTransmitted,
            packetsReceived = packetsReceived,
            isSuccess = packetsReceived > 0
        )
    }
}
