package com.flowkode.hfa.hub


data class User(
    val id: String,
    val login: String,
    val name: String,
    val groups: List<UserGroup>
)
