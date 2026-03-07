package com.ua.astrumon.domain.model

data class Group(
    val id: Long,
    val name: String,
    val memberUsernames: List<String>
)
