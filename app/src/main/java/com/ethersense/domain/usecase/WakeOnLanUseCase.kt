package com.ethersense.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

class WakeOnLanUseCase @Inject constructor() {

    companion object {
        private const val WOL_PORT = 9
        private const val MAGIC_PACKET_HEADER_SIZE = 6
        private const val MAC_ADDRESS_REPETITIONS = 16
        private const val MAC_ADDRESS_LENGTH = 6
    }

    suspend operator fun invoke(
        macAddress: String,
        broadcastAddress: String = "255.255.255.255",
        port: Int = WOL_PORT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanMac = cleanMacAddress(macAddress)

            if (!isValidMacAddress(cleanMac)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid MAC address format"))
            }

            val macBytes = parseMacAddress(cleanMac)
            val magicPacket = createMagicPacket(macBytes)

            sendMagicPacket(magicPacket, broadcastAddress, port)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanMacAddress(macAddress: String): String {
        return macAddress
            .uppercase()
            .replace(Regex("[^0-9A-F]"), "")
    }

    private fun isValidMacAddress(cleanMac: String): Boolean {
        return cleanMac.length == 12 && cleanMac.all { it in '0'..'9' || it in 'A'..'F' }
    }

    private fun parseMacAddress(cleanMac: String): ByteArray {
        return ByteArray(MAC_ADDRESS_LENGTH) { i ->
            cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun createMagicPacket(macBytes: ByteArray): ByteArray {
        val packetSize = MAGIC_PACKET_HEADER_SIZE + (MAC_ADDRESS_LENGTH * MAC_ADDRESS_REPETITIONS)
        val packet = ByteArray(packetSize)

        // First 6 bytes are 0xFF
        for (i in 0 until MAGIC_PACKET_HEADER_SIZE) {
            packet[i] = 0xFF.toByte()
        }

        // Followed by 16 repetitions of the MAC address
        for (i in 0 until MAC_ADDRESS_REPETITIONS) {
            val offset = MAGIC_PACKET_HEADER_SIZE + (i * MAC_ADDRESS_LENGTH)
            System.arraycopy(macBytes, 0, packet, offset, MAC_ADDRESS_LENGTH)
        }

        return packet
    }

    private fun sendMagicPacket(packet: ByteArray, broadcastAddress: String, port: Int) {
        DatagramSocket().use { socket ->
            socket.broadcast = true

            val address = InetAddress.getByName(broadcastAddress)
            val datagramPacket = DatagramPacket(packet, packet.size, address, port)

            socket.send(datagramPacket)
        }
    }

    fun formatMacAddress(input: String): String {
        val clean = cleanMacAddress(input)
        if (clean.length != 12) return input

        return clean.chunked(2).joinToString(":")
    }
}
