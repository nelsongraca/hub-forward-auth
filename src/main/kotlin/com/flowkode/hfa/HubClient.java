package com.flowkode.hfa;

import com.flowkode.hfa.hub.HeaderITem;
import com.flowkode.hfa.hub.UserGroupsResponse;
import io.quarkus.cache.CacheResult;
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/rest/")
@RegisterRestClient(configKey = "hub-client")
//@AccessToken
public interface HubClient {

    @GET
    @Path("/usergroups")
    @ClientQueryParam(name = "fields", value = "total,type,id,name")
    @CacheResult(cacheName = "hub-cache")
    UserGroupsResponse getUserGroups(
            @QueryParam("$skip") int start,
            @QueryParam("$top") int limit,
            @HeaderParam("Authorization") String authorization
    );
    @GET
    @Path("/services/header")
    @ClientQueryParam(name = "fields", value = "id,name,homeUrl")
    @CacheResult(cacheName = "hub-cache")
    List<HeaderITem> getHeader(
            @HeaderParam("Authorization") String authorization
    );
}
