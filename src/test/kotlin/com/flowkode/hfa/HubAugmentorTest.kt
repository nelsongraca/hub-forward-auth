package com.flowkode.hfa

import com.flowkode.hfa.hub.*
import io.quarkus.test.InjectMock
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import io.quarkus.test.junit.mockito.MockitoConfig
import io.quarkus.test.oidc.server.OidcWiremockTestResource
import io.restassured.RestAssured
import io.smallrye.jwt.build.Jwt
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
                listOf(UserGroup("admin", null))
            )
        ).`when`(hubclient)
            .getUser(ArgumentMatchers.anyString())

        Mockito.doReturn(
            UserGroupsResponse(
                0,
                1,
                1,
                listOf(UserGroup("admin", "admin"))
            )
        ).`when`(hubclient)
            .getUserGroups(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())

        Mockito.doReturn(
            listOf(HeaderItem("service", "service", "http://some.host.local/"))
        ).`when`(hubclient)
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
            .auth().oauth2(token)
            .redirects().follow(false)
            .`when`().get()
            .then()
            .statusCode(200)

        Mockito.verify(hubclient, Mockito.atMostOnce()).getUserGroups(1, 1000)
        Mockito.verify(hubclient, Mockito.atMostOnce()).getUser("someUser")
        Mockito.verify(hubclient, Mockito.atMostOnce()).getHeader()
    }
}