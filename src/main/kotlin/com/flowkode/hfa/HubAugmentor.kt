package com.flowkode.hfa

import io.quarkus.oidc.AccessTokenCredential
import io.quarkus.security.credential.TokenCredential
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.SecurityIdentityAugmentor
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.function.Supplier


@ApplicationScoped
class HubAugmentor : SecurityIdentityAugmentor {

    @RestClient
    lateinit var hubClient: HubClient

    override fun augment(identity: SecurityIdentity, context: AuthenticationRequestContext): Uni<SecurityIdentity> {
        return context.runBlocking(build(identity));
    }

    private fun build(identity: SecurityIdentity): Supplier<SecurityIdentity> {
        return if (identity.isAnonymous) {
            Supplier { identity }
        } else {
            // create a new builder and copy principal, attributes, credentials and roles from the original identity
            Supplier {
                val builder = QuarkusSecurityIdentity.builder(identity)
                val accessToken = identity.credentials.first { a -> a is AccessTokenCredential }
                if (accessToken != null) {
                    val token = "Bearer " + (accessToken as TokenCredential).token
                    val groups = hubClient.getUserGroups(1, 1000, token)
                    for (userGroup in groups.userGroups) {
                        builder.addRole(userGroup.name)
                    }
                    val services = hubClient.getHeader(token).map { it.homeUrl }.toSet()
                    builder.addAttribute("serviceUrls", services)
                }
                builder.build()
            }
        }
    }
}