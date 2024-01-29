package com.flowkode.hfa.hub

import com.fasterxml.jackson.annotation.JsonProperty


data class UserGroupsResponse(
    val skip: Int,
    val top: Int,
    val total: Int,
    @JsonProperty("usergroups") val userGroups: List<UserGroup>
)
