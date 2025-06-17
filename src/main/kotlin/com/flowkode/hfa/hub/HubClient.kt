package com.flowkode.hfa.hub

import io.quarkus.cache.CacheKey
import io.quarkus.cache.CacheName
import io.quarkus.cache.CacheResult
import io.quarkus.oidc.token.propagation.common.AccessToken
import io.quarkus.rest.client.reactive.ClientQueryParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/api/rest/")
@RegisterRestClient(configKey = "hub-client")
@AccessToken
interface HubClient {

    @GET
    @Path("/users/{userId}")
    @ClientQueryParam(name = "fields", value = ["id,login,name,groups(id,name)"])
//    @CacheResult(cacheName = "hub-cache")
    fun getUser(@PathParam("userId") userId: String): User

    @GET
    @Path("/users/me")
    @ClientQueryParam(name = "fields", value = ["id,login,name,groups(id,name)"])
    fun getMe(): User

    @GET
    @Path("/services/header")
    @ClientQueryParam(name = "fields", value = ["id,name,homeUrl"])
//    @CacheResult(cacheName = "hub-cache")
    fun getHeader(): List<HeaderItem?>
}
