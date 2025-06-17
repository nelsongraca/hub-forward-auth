package com.flowkode.hfa

import com.flowkode.hfa.hub.HubClient
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.quarkus.test.InjectMock
import io.quarkus.test.TestReactiveTransaction
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.MockitoConfig
import io.quarkus.test.security.TestSecurity
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test


class WireMockExtensions() : QuarkusTestResourceLifecycleManager {

    private var wireMockServer: WireMockServer = WireMockServer()

    override fun start(): Map<String, String> {
        wireMockServer.start()

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/rest/usergroups"))

        )

        wireMockServer.stubFor(
            WireMock.get(WireMock.urlMatching(".*"))
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(401)
                )
        )

        return java.util.Map.of("quarkus.rest-client.hub-client.url", wireMockServer.baseUrl())
    }

    override fun stop() {
        wireMockServer.stop()
    }
}

@QuarkusTest
@QuarkusTestResource(WireMockExtensions::class)
@TestSecurity(user = "cenas")

class CenasTest {


    @RestClient
    lateinit var hubclient: HubClient

    @Test
    @Disabled
    fun meh() {
        try {
            hubclient.getUser("randomId")

        }
        catch (e:Exception) {
            println("asd")
        }
    }

}

