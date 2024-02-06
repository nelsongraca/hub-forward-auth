package com.flowkode.hfa

import com.google.common.net.HttpHeaders.*
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.NewCookie
import org.junit.jupiter.api.Assertions

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtilTest {

    private var baseHeaderMap: MultivaluedHashMap<String, String> = MultivaluedHashMap()
    private var secureUtil: Util = Util("some.domain.local", 443, "some.domain.local", "", true)
    private var insecureUtil: Util = Util("some.domain.local", 80, "some.domain.local", "", false)

    @BeforeEach
    fun setUp() {
        baseHeaderMap = MultivaluedHashMap(
            mutableMapOf(
                Pair(X_FORWARDED_PROTO, "https"),
                Pair(X_FORWARDED_HOST, "some.domain.local"),
                Pair(X_FORWARDED_PORT, "443"),
                Pair(X_FORWARDED_URI, "/something")
            )
        )
        secureUtil = Util("some.domain.local", 443, "some.domain.local", "", true)
        insecureUtil = Util("some.domain.local", 80, "some.domain.local", "", false)
    }

    @Test
    fun buildUrlFromHeadersBase() {
        Assertions.assertEquals(
            "https://some.domain.local/something",
            secureUtil.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersBaseContext() {
        Assertions.assertEquals(
            "https://some.domain.local/something",
            secureUtil.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersBaseNoPath() {
        baseHeaderMap[X_FORWARDED_URI] = listOf("")
        Assertions.assertEquals(
            "https://some.domain.local/",
            secureUtil.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersHttpsDifferentPort() {
        baseHeaderMap[X_FORWARDED_PORT] = listOf("8443")
        Assertions.assertEquals(
            "https://some.domain.local:8443/something",
            secureUtil.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersHttpDifferentPort() {
        baseHeaderMap[X_FORWARDED_PROTO] = listOf("http")
        baseHeaderMap[X_FORWARDED_PORT] = listOf("8080")
        Assertions.assertEquals(
            "http://some.domain.local:8080/something",
            secureUtil.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersNoProtocol() {
        baseHeaderMap[X_FORWARDED_PROTO] = listOf("")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { secureUtil.buildUrlFromForwardHeaders(baseHeaderMap) }
    }

    @Test
    fun buildUrlFromHeadersNoHost() {
        baseHeaderMap[X_FORWARDED_HOST] = listOf("")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { secureUtil.buildUrlFromForwardHeaders(baseHeaderMap) }
    }


    @Test
    fun buildUrlFromHeadersHostWhitespace() {
        baseHeaderMap[X_FORWARDED_HOST] = listOf("  ")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { secureUtil.buildUrlFromForwardHeaders(baseHeaderMap) }
    }

    @Test
    fun buildUrlBadProtocol() {
        baseHeaderMap[X_FORWARDED_PROTO] = listOf("ftp")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { secureUtil.buildUrlFromForwardHeaders(baseHeaderMap) }
    }

    @Test
    fun urlIsSelfHttps() {
        Assertions.assertTrue(secureUtil.urlIsSelf("https://some.domain.local/"))
    }

    @Test
    fun urlIsSelfHttpsAltPort() {
        secureUtil = Util("some.domain.local", 8443, "", "", true)
        Assertions.assertTrue(secureUtil.urlIsSelf("https://some.domain.local:8443/"))
    }

    @Test
    fun urlIsSelfHttp() {
        Assertions.assertTrue(insecureUtil.urlIsSelf("http://some.domain.local/"))
    }

    @Test
    fun urlIsSelfHttpAltPort() {
        insecureUtil = Util("some.domain.local", 8080, "", "", false)
        Assertions.assertTrue(insecureUtil.urlIsSelf("http://some.domain.local:8080/"))
    }

    @Test
    fun returnCookie() {
        val url = "https://some.domain.local/"
        val cookie = NewCookie.Builder("return")
            .value(url)
            .domain("some.domain.local")
            .maxAge(60 * 5)
            .secure(true)
            .build()
        Assertions.assertEquals(cookie, secureUtil.returnCookie(url))
        Assertions.assertNotEquals(cookie, secureUtil.returnCookie("${url}la"))
    }

    @Test
    fun returnCookieClear() {
        val cookie = NewCookie.Builder("return")
            .value("")
            .domain("some.domain.local")
            .maxAge(0)
            .secure(true)
            .build()
        Assertions.assertEquals(cookie, secureUtil.returnCookie(null))
    }

    @Test
    fun returnInsecureCookie() {
        val url = "http://some.domain.local/"
        val cookie = NewCookie.Builder("return")
            .value(url)
            .domain("some.domain.local")
            .maxAge(60 * 5)
            .secure(false)
            .build()
        Assertions.assertEquals(cookie, insecureUtil.returnCookie(url))
        Assertions.assertNotEquals(cookie, insecureUtil.returnCookie("${url}la"))
    }

    @Test
    fun returnInsecureCookieClear() {
        val cookie = NewCookie.Builder("return")
            .value("")
            .domain("some.domain.local")
            .maxAge(0)
            .secure(false)
            .build()
        Assertions.assertEquals(cookie, insecureUtil.returnCookie(null))
    }

    @Test
    fun testWhitelistInRange() {
        val util = Util("", 443, "", "10.0.0.0/8", true)

        Assertions.assertTrue(util.isWhiteListed("10.0.0.0"))
        Assertions.assertTrue(util.isWhiteListed("10.255.255.255"))

        Assertions.assertFalse(util.isWhiteListed("192.168.1.1"))
        Assertions.assertFalse(util.isWhiteListed(""))
        Assertions.assertFalse(util.isWhiteListed("    "))
        Assertions.assertFalse(util.isWhiteListed("\n"))
        Assertions.assertFalse(util.isWhiteListed("\t"))
        Assertions.assertFalse(util.isWhiteListed("\t"))
    }

    @Test
    fun testEmptyWhitelist() {
        val util = Util("", 443, "", "", true)
        Assertions.assertFalse(util.isWhiteListed("10.0.0.0"))
    }

    @Test
    fun testNullWhitelist() {
        val util = Util("", 443, "", null, true)
        Assertions.assertFalse(util.isWhiteListed("10.0.0.0"))
    }

    @Test
    fun testInvalidWhitelist() {
        val util = Util("", 443, "", "10", true)
        Assertions.assertFalse(util.isWhiteListed("10.0.0.0"))
    }
}