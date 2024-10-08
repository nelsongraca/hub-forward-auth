package com.flowkode.hfa

import com.flowkode.hfa.hub.HubClient
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory
import java.util.function.Supplier


@ApplicationScoped
class HubAugmentor : SecurityIdentityAugmentor {

    companion object {

        const val SERVICE_URLS = "serviceUrls"
    }

    @RestClient
    lateinit var hubClient: HubClient

    private val logger = LoggerFactory.getLogger(HubAugmentor::class.java)

    override fun augment(identity: SecurityIdentity, context: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return context.runBlocking(build(identity))
    }

    private fun build(identity: SecurityIdentity): Supplier<SecurityIdentity> {
        return if (identity.isAnonymous) {
            Supplier { identity }
        }
        else {
            // create a new builder and copy principal, attributes, credentials and roles from the original identity
            Supplier {
                val builder = QuarkusSecurityIdentity.builder(identity)
                try {
                    val groups = hubClient.getUserGroups(1, 1000).userGroups
                    val user = hubClient.getUser((identity.principal as OidcJwtCallerPrincipal).subject)
                    user.groups.forEach { g -> g.name = groups.find { it.id == g.id }?.name }
                    for (userGroup in user.groups) {
                        builder.addRole(userGroup.name)
                    }
                    val services = hubClient.getHeader()
                        .filter { !it?.homeUrl.isNullOrBlank() }
                        .filterNotNull()
                        .map { it.homeUrl }
                        .toSet()
                    if (services.isNotEmpty())
                        builder.addAttribute(SERVICE_URLS, services)
                }
                catch (ex: WebApplicationException) {
                    logger.warn("Failed to fetch info for user: {}", identity.principal.name)
                    builder.setAnonymous(true)
                        .build()
                }
                builder.build()
            }
        }
    }
}
