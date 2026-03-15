package com.ua.astrumon.domain.repository

import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import kotlinx.datetime.Instant

interface MemberRepository {
    suspend fun findByUsername(username: String): ResultContainer<Member?>
    suspend fun findByUserId(userId: Long): ResultContainer<Member?>
    suspend fun save(userId: Long, username: String, firstName: String, joinedAt: Instant?): ResultContainer<Member>
    suspend fun findAll(): ResultContainer<List<Member>>
}