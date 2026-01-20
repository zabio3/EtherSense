package com.ethersense.data.model

data class PortScanResult(
    val host: String,
    val port: Int,
    val status: PortStatus,
    val serviceName: String? = null,
    val responseTime: Long? = null
)

enum class PortStatus {
    OPEN,
    CLOSED,
    FILTERED
}

data class PortScanProgress(
    val currentPort: Int,
    val totalPorts: Int,
    val openPorts: Int,
    val closedPorts: Int,
    val filteredPorts: Int
)

object CommonPorts {
    val COMMON_PORTS = listOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS",
        80 to "HTTP",
        110 to "POP3",
        143 to "IMAP",
        443 to "HTTPS",
        445 to "SMB",
        993 to "IMAPS",
        995 to "POP3S",
        3306 to "MySQL",
        3389 to "RDP",
        5432 to "PostgreSQL",
        5900 to "VNC",
        6379 to "Redis",
        8080 to "HTTP Alt",
        8443 to "HTTPS Alt",
        27017 to "MongoDB"
    )

    fun getServiceName(port: Int): String? {
        return COMMON_PORTS.find { it.first == port }?.second
    }
}
