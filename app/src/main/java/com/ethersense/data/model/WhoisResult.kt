package com.ethersense.data.model

data class WhoisResult(
    val query: String,
    val queryType: WhoisQueryType,
    val rawResponse: String,
    val parsedInfo: WhoisParsedInfo? = null,
    val server: String,
    val queryTime: Long,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

enum class WhoisQueryType {
    DOMAIN,
    IP_ADDRESS,
    AS_NUMBER
}

data class WhoisParsedInfo(
    val domainName: String? = null,
    val registrar: String? = null,
    val creationDate: String? = null,
    val expirationDate: String? = null,
    val updatedDate: String? = null,
    val nameServers: List<String> = emptyList(),
    val status: List<String> = emptyList(),
    val organization: String? = null,
    val country: String? = null
)
