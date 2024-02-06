package com.flowkode.hfa

import io.quarkus.vertx.web.RouteFilter
import io.vertx.ext.web.RoutingContext
import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.NewCookie
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.RuntimeDelegate
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.resteasy.reactive.server.ServerResponseFilter


class AuxFilter(
    @ConfigProperty(name = "auth.domain") val authDomain: String,
    @Inject var util: Util
) {
    @RouteFilter(100)
    fun hostChangeFilter(rc: RoutingContext) {
        rc.request().headers().set("Host", authDomain)
        rc.next()
    }

    @ServerResponseFilter
    fun returnCookieFilter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
        try {
            if (responseContext.status == 302) {
                responseContext.status = Response.Status.TEMPORARY_REDIRECT.statusCode;
            }
            val returnAddress = util.buildUrlFromForwardHeaders(requestContext.headers)
            if (util.urlIsSelf(returnAddress) || (responseContext.status in 200..299)) {
                return
            }
            val encodedCokie = RuntimeDelegate.getInstance()
                .createHeaderDelegate(NewCookie::class.java)
                .toString(util.returnCookie(returnAddress))
            responseContext.headers.add("Set-Cookie", encodedCokie)
        } catch (_: IllegalArgumentException) {
            // do nothing
        }

    }

}


