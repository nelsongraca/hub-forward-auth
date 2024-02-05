package com.flowkode.hfa

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.NewCookie
import org.apache.commons.net.util.SubnetUtils
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*


@ApplicationScoped
class Util(
    @ConfigProperty(name = "auth.domain") val authDomain: String,
    @ConfigProperty(name = "cookie.domain") val cookieDomain: String,
    @ConfigProperty(name = "whitelist") val whitelistNetworks: String,
    @ConfigProperty(name = "secure") val secure: Boolean
) {
    companion object {
        private const val COOKIE_NAME = "return"
    }

    val whiteList: List<SubnetUtils> = whitelistNetworks.split(",")
        .map { if (it.contains("/")) it else "$it/32" }
        .map { SubnetUtils(it) }
        .onEach { su -> su.isInclusiveHostCount = true }

    @Throws(IllegalArgumentException::class)
    fun buildAddress(protocol: String?, host: String?, port: String?, requestedURI: String?): String {
        if (protocol == null) {
            throw IllegalArgumentException()
        }
        if (host == null) {
            throw IllegalArgumentException()
        }

        val isHttp = "http".equals(protocol, true)
        val isHttps = "https".equals(protocol, true)

        if (!isHttp && !isHttps) {
            throw IllegalArgumentException()
        }

        val p = if ((isHttp && "80" == port) || isHttps && "443" == port) "" else ":${port.orEmpty()}"

        return "${protocol}://${host}${p}${requestedURI.orEmpty()}"
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

    fun buildUrlFromForwardHeaders(headers: HttpHeaders): String {
        return buildAddress(
            headers.getHeaderString("X-Forwarded-Proto"),
            headers.getHeaderString("X-Forwarded-Host"),
            headers.getHeaderString("X-Forwarded-Port"),
            headers.getHeaderString("X-Forwarded-Uri")
        )
    }

    fun buildUrlFromForwardHeaders(requestContext: ContainerRequestContext): String {
        return buildAddress(
            requestContext.headers["X-Forwarded-Proto"]?.firstOrNull(),
            requestContext.headers["X-Forwarded-Host"]?.firstOrNull(),
            requestContext.headers["X-Forwarded-Port"]?.firstOrNull(),
            requestContext.headers["X-Forwarded-Uri"]?.firstOrNull()
        )
    }

    fun isWhiteListed(headerString: String?): Boolean {
        if (headerString.isNullOrBlank()) {
            return false
        }
        return whiteList.any { it.info.isInRange(headerString) }
    }
}