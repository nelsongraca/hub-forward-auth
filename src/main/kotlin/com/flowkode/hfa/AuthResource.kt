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
import java.net.URI


@Path("/")
@ApplicationScoped
class AuthResource(
    @Inject var securityIdentity: SecurityIdentity,
    @Inject var util: Util
) {

    @GET
    fun root(@Context headers: HttpHeaders, @CookieParam(COOKIE_NAME) returnUrl: String?): Response {
        if (util.isWhiteListed(headers.getHeaderString(X_FORWARDED_FOR)))
            return Response.ok().build()

        if (securityIdentity.isAnonymous) {
            throw UnauthorizedException()
        }
        val url = util.buildUrlFromForwardHeaders(headers.requestHeaders)
        return if (util.urlIsSelf(url) && returnUrl != null) {
            Response.temporaryRedirect(URI(returnUrl)).cookie(util.returnCookie(null)).build()
        } else {
            val serviceUrls = securityIdentity.attributes[SERVICE_URLS]
            if (serviceUrls != null && (serviceUrls as Set<String>).any { url.startsWith(it) }) {
                Response.ok()
                    .header(X_FORWARDED_USER, securityIdentity.principal.name)
                    .build()
            } else {
                Response.status(Response.Status.FORBIDDEN).build()
            }
        }
    }
}