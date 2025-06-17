package com.flowkode.hfa

import com.flowkode.hfa.hub.*
import io.quarkus.test.InjectMock
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.MockitoConfig
import io.quarkus.test.oidc.server.OidcWiremockTestResource
import io.restassured.RestAssured
import io.smallrye.jwt.build.Jwt
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

@QuarkusTest
@TestHTTPEndpoint(AuthResource::class)
@QuarkusTestResource(OidcWiremockTestResource::class)
class HubAugmentorTest {

    @InjectMock
    @MockitoConfig(convertScopes = true)
    @RestClient
    lateinit var hubclient: HubClient


    @Test
    fun tokenPropagation() {
        val token = Jwt.preferredUserName("someUser")
            .issuer("https://server.example.com")
            .audience("https://service.example.com")
            .subject("sommeUser")
            .sign();

        Mockito.doReturn(
            User(
                "userId",
                "name",
                name = "Name",
                listOf(UserGroup("admin", "Admins"))
            )
        )
            .`when`(hubclient)
            .getUser(ArgumentMatchers.anyString())

        Mockito.doReturn(
            listOf(HeaderItem("service", "service", "http://some.host.local/"))
        )
            .`when`(hubclient)
            .getHeader()

        RestAssured.given()
            .headers(
                "X-Forwarded-Method", "GET",
                "X-Forwarded-Proto", "http",
                "X-Forwarded-Port", "80",
                "X-Forwarded-Host", "some.host.local",
                "X-Forwarded-Uri", "/",
                "X-Forwarded-For", "10.0.0.1"
            )
            .auth()
            .oauth2(token)
            .redirects()
            .follow(false)
            .`when`()
            .get()
            .then()
            .statusCode(200)

        Mockito.verify(hubclient, Mockito.atMostOnce())
            .getUser("someUser")
        Mockito.verify(hubclient, Mockito.atMostOnce())
            .getHeader()
    }

    @Test
    fun handle401FromClient() {
        val token = Jwt.preferredUserName("someUser")
            .issuer("https://server.example.com")
            .audience("https://service.example.com")
            .subject("sommeUser")
            .sign();

        Mockito.doThrow(
            WebApplicationException(401)
        )
            .`when`(hubclient)
            .getUser(ArgumentMatchers.anyString())

        Mockito.doThrow(
            WebApplicationException(401)
        )
            .`when`(hubclient)
            .getHeader()

        RestAssured.given()
            .headers(
                "X-Forwarded-Method", "GET",
                "X-Forwarded-Proto", "http",
                "X-Forwarded-Port", "80",
                "X-Forwarded-Host", "some.host.local",
                "X-Forwarded-Uri", "/",
                "X-Forwarded-For", "10.0.0.1"
            )
            .auth()
            .oauth2(token)
            .redirects()
            .follow(false)
            .`when`()
            .get()
            .then()
            .statusCode(401)

        Mockito.verify(hubclient, Mockito.atMostOnce())
            .getUser("someUser")
        Mockito.verify(hubclient, Mockito.atMostOnce())
            .getHeader()
    }
}