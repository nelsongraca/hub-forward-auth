package com.flowkode.hfa

import io.quarkus.security.UnauthorizedException
import io.quarkus.security.identity.SecurityIdentity
import jakarta.inject.Inject
import jakarta.ws.rs.CookieParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import java.net.URI


@Path("/")
class AuthResource(
    @Inject var securityIdentity: SecurityIdentity,
    @Inject var util: Util
) {

    @GET
    fun root(@Context headers: HttpHeaders, @CookieParam("return") returnUrl: String?): Response {
        if (util.isWhiteListed(headers.getHeaderString("X-Forwarded-For")))
            return Response.ok().build()
        try {
            if (securityIdentity.isAnonymous) {
                throw UnauthorizedException()
            }
            val url = util.buildUrlFromForwardHeaders(headers.requestHeaders)
            return if (util.urlIsSelf(url) && returnUrl != null) {
                Response.temporaryRedirect(URI(returnUrl)).cookie(util.returnCookie(null)).build()
            } else if ((securityIdentity.attributes["serviceUrls"] as Set<String>).any { url.startsWith(it) }) {
                Response.ok()
                    .header("X-Forwarded-User", securityIdentity.principal.name)
                    .build()
            } else {
                Response.status(Response.Status.FORBIDDEN).build()
            }
        } catch (e: NotAllowedException) {
            return Response.status(Response.Status.METHOD_NOT_ALLOWED).build()
        }
    }
}