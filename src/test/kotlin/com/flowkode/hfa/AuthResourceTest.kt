package com.flowkode.hfa

import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.SecurityAttribute
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.TestSecurityIdentityAugmentor
import io.restassured.RestAssured
import io.smallrye.jwt.build.Jwt
import jakarta.enterprise.context.ApplicationScoped
import org.junit.jupiter.api.Test


@QuarkusTest
@TestHTTPEndpoint(AuthResource::class)
class AuthResourceTest {

//
//    @InjectMock
//    @MockitoConfig(convertScopes = true)
//    @RestClient
//    lateinit var hubclient: HubClient


    //        Mockito.doReturn(
//            UserGroupsResponse(
//                0,
//                1,
//                1,
//                listOf(UserGroup("admin", "admin"))
//            )
//        )
//            .`when`(hubclient)
//            .getUserGroups(anyInt(), anyInt(), anyString())
//
//       Mockito.doReturn(
//          listOf(HeaderItem("service","service","http://some.host.local/"))
//        )
//            .`when`(hubclient)
//            .getHeader(anyString())


    @Test
    @TestSecurity(user = "someUser", attributes = [SecurityAttribute(key = "urls", value = "http://some.host.local/")])
    fun hasAccessToUrl() {

        RestAssured.given()
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
            .statusCode(200)
            .header("X-Forwarded-User","someUser")
    }

    @Test
    @TestSecurity(user = "someUser", attributes = [SecurityAttribute(key = "urls", value = "http://other.host.local/")])
    fun doesNotHasAccessToUrl() {
        RestAssured.given()
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
            .statusCode(403)
    }

    @Test
    @TestSecurity(user = "someUser", attributes = [SecurityAttribute(key = "urls", value = "https://some.host.local/")])
    fun protocolDoesNotMatch() {
        RestAssured.given()
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
            .statusCode(403)
    }
    @Test
    @TestSecurity(user = "someUser")
    fun noServiceUrls() {
        RestAssured.given()
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
            .statusCode(403)
    }


}