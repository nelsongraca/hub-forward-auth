package com.flowkode.hfa

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.NewCookie
import org.apache.commons.net.util.SubnetUtils
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*


private const val X_FORWARDED_PROTO = "X-Forwarded-Proto"

private const val X_FORWARDED_HOST = "X-Forwarded-Host"

private const val X_FORWARDED_PORT = "X-Forwarded-Port"

const val X_FORWARDED_URI = "X-Forwarded-Uri"

@ApplicationScoped
class Util(
    @ConfigProperty(name = "auth.domain") val authDomain: String,
    @ConfigProperty(name = "cookie.domain") val cookieDomain: String,
    @ConfigProperty(name = "whitelist") val whitelistNetworks: String?,
    @ConfigProperty(name = "secure") val secure: Boolean
) {
    companion object {
        private const val COOKIE_NAME = "return"
    }

    val whiteList: List<SubnetUtils> = whitelistNetworks.orEmpty()
        .split(",")
        .asSequence()
        .filter { it.isNotBlank() }
        .map { if (it.contains("/")) it else "$it/32" }
        .map { runCatching { SubnetUtils(it) } }
        .mapNotNull { it.getOrNull() }
        .onEach { su -> su.isInclusiveHostCount = true }
        .toList()

    @Throws(IllegalArgumentException::class)
    private fun buildAddress(protocol: String, host: String, port: String, requestedURI: String): String {
        if (protocol.isEmpty()) {
            throw IllegalArgumentException()
        }
        if (host.isEmpty()) {
            throw IllegalArgumentException()
        }

        val isHttp = "http".equals(protocol, true)
        val isHttps = "https".equals(protocol, true)

        if (!isHttp && !isHttps) {
            throw IllegalArgumentException()
        }
        val path = requestedURI.ifEmpty { "/" }

        val p = if ((isHttp && "80" == port) || isHttps && "443" == port) "" else ":${port}"

        return "${protocol}://${host}${p}${path}"
    }

    fun urlIsSelf(url: String): Boolean {
        val sec = if (secure) "s" else ""

        return url.startsWith("http$sec://$authDomain")
    }

    fun returnCookie(url: String?): NewCookie {
        return NewCookie.Builder(COOKIE_NAME)
            .value(url)
            .domain(cookieDomain)
            .maxAge(if (url == null) 0 else 60 * 5)
            .secure(secure)
            .build()
    }

    fun buildUrlFromForwardHeaders(headers: MultivaluedMap<String, String>): String {
        return buildAddress(
            headers[X_FORWARDED_PROTO].firstOrEmpty().trim(),
            headers[X_FORWARDED_HOST].firstOrEmpty().trim(),
            headers[X_FORWARDED_PORT].firstOrEmpty().trim(),
            headers[X_FORWARDED_URI].firstOrEmpty().trim()
        )
    }

    fun isWhiteListed(headerString: String?): Boolean {
        if (headerString.isNullOrBlank()) {
            return false
        }
        return whiteList.any { it.info.isInRange(headerString) }
    }

}

public fun List<String>?.firstOrEmpty(): String {
    return if (isNullOrEmpty()) "" else this[0]
}