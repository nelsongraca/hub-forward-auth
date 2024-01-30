package com.flowkode.hfa.hub

import io.quarkus.cache.CacheResult
import io.quarkus.rest.client.reactive.ClientQueryParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/rest/")
@RegisterRestClient(configKey = "hub-client") //@AccessToken
interface HubClient {
    @GET
    @Path("/usergroups")
    @ClientQueryParam(name = "fields", value = ["total,type,id,name"])
    @CacheResult(cacheName = "hub-cache")
    fun getUserGroups(
        @QueryParam("\$skip") start: Int,
        @QueryParam("\$top") limit: Int,
        @HeaderParam("Authorization") authorization: String?
    ): UserGroupsResponse

    @GET
    @Path("/services/header")
    @ClientQueryParam(name = "fields", value = ["id,name,homeUrl"])
    @CacheResult(cacheName = "hub-cache")
    fun getHeader(
        @HeaderParam("Authorization") authorization: String?
    ): List<HeaderItem?>
}
