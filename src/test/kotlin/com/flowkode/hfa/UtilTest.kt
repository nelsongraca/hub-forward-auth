package com.flowkode.hfa

import com.google.common.net.HttpHeaders.*
import jakarta.ws.rs.core.MultivaluedHashMap
import org.junit.jupiter.api.Assertions

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UtilTest {

    private var baseHeaderMap: MultivaluedHashMap<String, String> = MultivaluedHashMap()
    private var util: Util = Util("", "", "", true)

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
        util = Util("", "", "", true)
    }

    @Test
    fun buildUrlFromHeadersBase() {
        Assertions.assertEquals(
            "https://some.domain.local/something",
            util.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersBaseContext() {
        Assertions.assertEquals(
            "https://some.domain.local/something",
            util.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersBaseNoPath() {
        baseHeaderMap[X_FORWARDED_URI] = listOf("")
        Assertions.assertEquals(
            "https://some.domain.local/",
            util.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersHttpsDifferentPort() {
        baseHeaderMap[X_FORWARDED_PORT] = listOf("8443")
        Assertions.assertEquals(
            "https://some.domain.local:8443/something",
            util.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersHttpDifferentPort() {
        baseHeaderMap[X_FORWARDED_PROTO] = listOf("http")
        baseHeaderMap[X_FORWARDED_PORT] = listOf("8080")
        Assertions.assertEquals(
            "http://some.domain.local:8080/something",
            util.buildUrlFromForwardHeaders(baseHeaderMap)
        )
    }

    @Test
    fun buildUrlFromHeadersNoProtocol() {
        baseHeaderMap[X_FORWARDED_PROTO] = listOf("")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { util.buildUrlFromForwardHeaders(baseHeaderMap) }
    }

    @Test
    fun buildUrlFromHeadersNoHost() {
        baseHeaderMap[X_FORWARDED_HOST] = listOf("")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { util.buildUrlFromForwardHeaders(baseHeaderMap) }
    }


    @Test
    fun buildUrlFromHeadersHostWhitespace() {
        baseHeaderMap[X_FORWARDED_HOST] = listOf("  ")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { util.buildUrlFromForwardHeaders(baseHeaderMap) }
    }

    @Test
    fun buildUrlBadProtocol() {
        baseHeaderMap[X_FORWARDED_PROTO] = listOf("ftp")
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) { util.buildUrlFromForwardHeaders(baseHeaderMap) }
    }



    @Test
    fun testWhitelistInRange() {
        val util = Util("", "", "10.0.0.0/8", true)

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
        val util = Util("", "", "", true)
        Assertions.assertFalse(util.isWhiteListed("10.0.0.0"))
    }

    @Test
    fun testNullWhitelist() {
        val util = Util("", "", null, true)
        Assertions.assertFalse(util.isWhiteListed("10.0.0.0"))
    }

    @Test
    fun testInvalidWhitelist() {
        val util = Util("", "", "10", true)
        Assertions.assertFalse(util.isWhiteListed("10.0.0.0"))
    }
}