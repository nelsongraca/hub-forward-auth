package com.flowkode.hfa

import io.quarkus.security.UnauthorizedException
import io.quarkus.security.identity.SecurityIdentity
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.CookieParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import java.net.URI


@Path("/")
@ApplicationScoped
class AuthResource(
    @Inject var securityIdentity: SecurityIdentity,
    @Inject var util: Util
) {

    private val logger = LoggerFactory.getLogger(AuthResource::class.java)

    @GET
    @Path("{path:.*}")
    fun root(@Context headers: HttpHeaders, @CookieParam(Util.COOKIE_NAME) returnUrl: String?): Response {
        val forwardedFor = headers.getHeaderString(Util.X_FORWARDED_FOR)
        if (util.isWhiteListed(forwardedFor))
            return Response.ok()
                .build()

        if (securityIdentity.isAnonymous) {
            logger.info("User is anonymous IP: $forwardedFor")
            throw UnauthorizedException()
        }
        val url = util.buildUrlFromForwardHeaders(headers.requestHeaders)
        return if (util.urlIsSelf(url) && returnUrl != null) {
            Response.temporaryRedirect(URI(returnUrl))
                .cookie(util.returnCookie(null))
                .build()
        }
        else {
            val serviceUrls = securityIdentity.attributes[HubAugmentor.SERVICE_URLS]
            if (serviceUrls != null && (serviceUrls as Set<String>).any { url.startsWith(it) }) {
                Response.ok()
                    .header(Util.X_FORWARDED_USER, securityIdentity.principal.name)
                    .header(Util.X_FORWARDED_GROUPS, securityIdentity.roles.joinToString(","))
                    .build()
            }
            else {
                Response.status(Response.Status.FORBIDDEN)
                    .build()
            }
        }
    }
}