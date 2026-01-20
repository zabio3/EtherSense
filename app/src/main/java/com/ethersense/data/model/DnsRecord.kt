package com.ethersense.data.model

data class DnsRecord(
    val type: DnsRecordType,
    val value: String,
    val ttl: Long? = null,
    val priority: Int? = null
)

enum class DnsRecordType(val displayName: String) {
    A("A (IPv4)"),
    AAAA("AAAA (IPv6)"),
    MX("MX (Mail)"),
    TXT("TXT (Text)"),
    NS("NS (Name Server)"),
    CNAME("CNAME (Alias)"),
    PTR("PTR (Pointer)"),
    SOA("SOA (Authority)"),
    SRV("SRV (Service)")
}

data class DnsLookupResult(
    val domain: String,
    val records: List<DnsRecord>,
    val queryTime: Long,
    val server: String? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
