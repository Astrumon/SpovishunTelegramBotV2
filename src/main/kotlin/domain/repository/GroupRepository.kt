package com.ua.astrumon.domain.repository

import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Group

interface GroupRepository {
    suspend fun getAllGroups(): ResultContainer<List<Group>>
    suspend fun findGroupByKey(key: String): ResultContainer<Group>
    suspend fun createGroup(name: String): ResultContainer<Group>
    suspend fun deleteGroup(key: String): ResultContainer<Unit>
}
