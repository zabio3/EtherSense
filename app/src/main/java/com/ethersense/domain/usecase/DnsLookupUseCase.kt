package com.ethersense.domain.usecase

import com.ethersense.data.model.DnsLookupResult
import com.ethersense.data.model.DnsRecord
import com.ethersense.data.model.DnsRecordType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject

class DnsLookupUseCase @Inject constructor() {

    companion object {
        private val SUPPORTED_TYPES = setOf(
            DnsRecordType.A,
            DnsRecordType.AAAA,
            DnsRecordType.PTR
        )
    }

    suspend operator fun invoke(
        domain: String,
        recordType: DnsRecordType = DnsRecordType.A
    ): DnsLookupResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Check if record type is supported
        if (recordType !in SUPPORTED_TYPES) {
            return@withContext DnsLookupResult(
                domain = domain,
                records = emptyList(),
                queryTime = System.currentTimeMillis() - startTime,
                isSuccess = false,
                errorMessage = "${recordType.displayName} lookup is not supported. Only A, AAAA, and PTR records are available."
            )
        }

        try {
            val records = when (recordType) {
                DnsRecordType.A -> lookupA(domain)
                DnsRecordType.AAAA -> lookupAAAA(domain)
                DnsRecordType.PTR -> lookupPTR(domain)
                else -> emptyList()
            }

            val queryTime = System.currentTimeMillis() - startTime

            DnsLookupResult(
                domain = domain,
                records = records,
                queryTime = queryTime,
                isSuccess = records.isNotEmpty(),
                errorMessage = if (records.isEmpty()) "No records found" else null
            )
        } catch (e: Exception) {
            DnsLookupResult(
                domain = domain,
                records = emptyList(),
                queryTime = System.currentTimeMillis() - startTime,
                isSuccess = false,
                errorMessage = e.message ?: "DNS lookup failed"
            )
        }
    }

    suspend fun lookupAll(domain: String): DnsLookupResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val records = mutableListOf<DnsRecord>()

            // A records (IPv4)
            records.addAll(lookupA(domain))

            // AAAA records (IPv6)
            records.addAll(lookupAAAA(domain))

            val queryTime = System.currentTimeMillis() - startTime

            DnsLookupResult(
                domain = domain,
                records = records,
                queryTime = queryTime,
                isSuccess = records.isNotEmpty()
            )
        } catch (e: Exception) {
            DnsLookupResult(
                domain = domain,
                records = emptyList(),
                queryTime = System.currentTimeMillis() - startTime,
                isSuccess = false,
                errorMessage = e.message ?: "DNS lookup failed"
            )
        }
    }

    private fun lookupA(domain: String): List<DnsRecord> {
        return try {
            InetAddress.getAllByName(domain)
                .filter { it.address.size == 4 } // IPv4 only
                .map { address ->
                    DnsRecord(
                        type = DnsRecordType.A,
                        value = address.hostAddress ?: ""
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun lookupAAAA(domain: String): List<DnsRecord> {
        return try {
            InetAddress.getAllByName(domain)
                .filter { it.address.size == 16 } // IPv6 only
                .map { address ->
                    DnsRecord(
                        type = DnsRecordType.AAAA,
                        value = address.hostAddress ?: ""
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun lookupPTR(ipAddress: String): List<DnsRecord> {
        return try {
            val address = InetAddress.getByName(ipAddress)
            val hostname = address.canonicalHostName
            if (hostname != ipAddress) {
                listOf(
                    DnsRecord(
                        type = DnsRecordType.PTR,
                        value = hostname
                    )
                )
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
