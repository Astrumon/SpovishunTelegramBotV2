package com.ua.astrumon.domain.repository

import com.ua.astrumon.common.result.ResultContainer

interface GroupMemberRepository {
    suspend fun addMemberToGroup(groupKey: String, username: String): ResultContainer<Unit>
    suspend fun removeMemberFromGroup(groupKey: String, username: String): ResultContainer<Unit>
    suspend fun getGroupMembers(groupKey: String): ResultContainer<List<String>>
}
