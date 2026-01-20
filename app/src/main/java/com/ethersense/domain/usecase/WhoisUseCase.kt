package com.ethersense.domain.usecase

import com.ethersense.data.model.WhoisParsedInfo
import com.ethersense.data.model.WhoisQueryType
import com.ethersense.data.model.WhoisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

class WhoisUseCase @Inject constructor() {

    companion object {
        private const val WHOIS_PORT = 43
        private const val TIMEOUT_MS = 10000

        private val WHOIS_SERVERS = mapOf(
            "com" to "whois.verisign-grs.com",
            "net" to "whois.verisign-grs.com",
            "org" to "whois.pir.org",
            "info" to "whois.afilias.net",
            "io" to "whois.nic.io",
            "co" to "whois.nic.co",
            "dev" to "whois.nic.google",
            "app" to "whois.nic.google",
            "jp" to "whois.jprs.jp"
        )

        private const val IANA_WHOIS = "whois.iana.org"
        private const val ARIN_WHOIS = "whois.arin.net"
    }

    suspend operator fun invoke(query: String): WhoisResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val queryType = detectQueryType(query)
        val server = getWhoisServer(query, queryType)

        try {
            val response = performWhoisQuery(query, server)
            val queryTime = System.currentTimeMillis() - startTime

            // Check if we need to follow a referral
            val referralServer = findReferralServer(response)
            val finalResponse = if (referralServer != null && referralServer != server) {
                performWhoisQuery(query, referralServer)
            } else {
                response
            }

            val parsedInfo = parseWhoisResponse(finalResponse, queryType)

            WhoisResult(
                query = query,
                queryType = queryType,
                rawResponse = finalResponse,
                parsedInfo = parsedInfo,
                server = referralServer ?: server,
                queryTime = queryTime,
                isSuccess = true
            )
        } catch (e: Exception) {
            WhoisResult(
                query = query,
                queryType = queryType,
                rawResponse = "",
                server = server,
                queryTime = System.currentTimeMillis() - startTime,
                isSuccess = false,
                errorMessage = e.message ?: "WHOIS query failed"
            )
        }
    }

    private fun detectQueryType(query: String): WhoisQueryType {
        return when {
            query.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")) -> WhoisQueryType.IP_ADDRESS
            query.startsWith("AS", ignoreCase = true) && query.drop(2).all { it.isDigit() } -> WhoisQueryType.AS_NUMBER
            else -> WhoisQueryType.DOMAIN
        }
    }

    private fun getWhoisServer(query: String, queryType: WhoisQueryType): String {
        return when (queryType) {
            WhoisQueryType.IP_ADDRESS -> ARIN_WHOIS
            WhoisQueryType.AS_NUMBER -> ARIN_WHOIS
            WhoisQueryType.DOMAIN -> {
                val tld = query.substringAfterLast(".")
                WHOIS_SERVERS[tld.lowercase()] ?: IANA_WHOIS
            }
        }
    }

    private fun performWhoisQuery(query: String, server: String): String {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(server, WHOIS_PORT), TIMEOUT_MS)
            socket.soTimeout = TIMEOUT_MS

            val writer = OutputStreamWriter(socket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            writer.write("$query\r\n")
            writer.flush()

            return reader.readText()
        }
    }

    private fun findReferralServer(response: String): String? {
        val patterns = listOf(
            Regex("""Registrar WHOIS Server:\s*(.+)""", RegexOption.IGNORE_CASE),
            Regex("""whois:\s*(.+)""", RegexOption.IGNORE_CASE),
            Regex("""ReferralServer:\s*whois://(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(response)?.let {
                return it.groupValues[1].trim()
            }
        }
        return null
    }

    private fun parseWhoisResponse(response: String, queryType: WhoisQueryType): WhoisParsedInfo? {
        if (queryType != WhoisQueryType.DOMAIN) return null

        val lines = response.lines()

        fun findValue(vararg keys: String): String? {
            for (key in keys) {
                lines.find { it.startsWith(key, ignoreCase = true) }?.let {
                    return it.substringAfter(":").trim()
                }
            }
            return null
        }

        fun findValues(vararg keys: String): List<String> {
            val results = mutableListOf<String>()
            for (key in keys) {
                lines.filter { it.startsWith(key, ignoreCase = true) }.forEach {
                    results.add(it.substringAfter(":").trim())
                }
            }
            return results
        }

        return WhoisParsedInfo(
            domainName = findValue("Domain Name"),
            registrar = findValue("Registrar", "Sponsoring Registrar"),
            creationDate = findValue("Creation Date", "Created Date", "Created"),
            expirationDate = findValue("Registry Expiry Date", "Expiration Date", "Expires"),
            updatedDate = findValue("Updated Date", "Last Updated"),
            nameServers = findValues("Name Server"),
            status = findValues("Domain Status", "Status"),
            organization = findValue("Registrant Organization", "Organization"),
            country = findValue("Registrant Country", "Country")
        )
    }
}
