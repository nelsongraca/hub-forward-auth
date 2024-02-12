package com.flowkode.hfa

import com.flowkode.hfa.hub.HubClient
import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import io.restassured.matcher.RestAssuredMatchers.detailedCookie
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern


@QuarkusTest
@TestHTTPEndpoint(AuthResource::class)
class AuxFilterTest {

    @InjectSpy
    lateinit var util: Util

    @Test
    fun hostChangeFilter() {
        given()
            .headers(
                "X-Forwarded-Method", "GET",
                "X-Forwarded-Proto", "http",
                "X-Forwarded-Port", "80",
                "X-Forwarded-Host", "some.host.local",
                "X-Forwarded-Uri", "/",
                "X-Forwarded-For", "10.0.0.1"
            )
            .redirects().follow(false)
            .`when`().get()
            .then()
            .statusCode(307)
            .cookie(
                "return",
                detailedCookie()
                    .value("\"http://some.host.local/\"")
                    .domain("host.local")
            )
            .header("Location", { decomposeQueryString(URI(it).query)["redirect_uri"] }, CoreMatchers.equalTo("http://auth.host.local/"))
    }

    @Test
    @TestSecurity(user = "randomUser")
    fun clearCookieWhenSelf() {
        given()
            .headers(
                "X-Forwarded-Method", "GET",
                "X-Forwarded-Proto", "http",
                "X-Forwarded-Port", "80",
                "X-Forwarded-Host", "auth.host.local",
                "X-Forwarded-Uri", "/",
                "X-Forwarded-For", "10.0.0.1"
            )
            .cookie("return", "http://some.host.local/")
            .redirects().follow(false)
            .`when`().get()
            .then()
            .statusCode(307)
            .cookie(
                "return", detailedCookie()
                    .value("")
                    .maxAge(0)
            )
            .header("Location", "http://some.host.local/")
    }

    private fun decomposeQueryString(query: String): Map<String, String?> {
        return query.split("&")
            .map { it.split(Pattern.compile("="), 2) }
            .associate {
                Pair(
                    URLDecoder.decode(it[0], StandardCharsets.UTF_8),
                    if (it.size > 1) URLDecoder.decode(it[1], StandardCharsets.UTF_8) else null
                )
            }
    }

    @Test
    fun testWhitelisted() {
        doReturn(true).`when`(util).isWhiteListed("10.0.0.1")

        given()
            .headers(
                "X-Forwarded-Method", "GET",
                "X-Forwarded-Proto", "http",
                "X-Forwarded-Port", "80",
                "X-Forwarded-Host", "some.host.local",
                "X-Forwarded-Uri", "/",
                "X-Forwarded-For", "10.0.0.1"
            )
            .`when`().get()
            .then()
            .statusCode(200)
    }

}