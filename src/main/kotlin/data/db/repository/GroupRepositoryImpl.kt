package com.ua.astrumon.data.db.repository

import com.ua.astrumon.common.extension.safeDbQuery
import com.ua.astrumon.common.exception.*
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.data.db.table.Groups
import com.ua.astrumon.domain.model.Group
import com.ua.astrumon.domain.repository.GroupRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.slf4j.LoggerFactory

class GroupRepositoryImpl : GroupRepository {
    private val logger = LoggerFactory.getLogger(GroupRepositoryImpl::class.java)
    
    override suspend fun getAllGroups(): ResultContainer<List<Group>> = safeDbQuery {
        Groups.selectAll().map { row ->
            Group(
                id = row[Groups.id].value,
                name = row[Groups.name],
                memberUsernames = emptyList()
            )
        }
    }
    
    override suspend fun findGroupByKey(key: String): ResultContainer<Group> {
        logger.info("DEBUG: findGroupByKey called with key: '$key'")
        return safeDbQuery {
            logger.info("DEBUG: Searching for group with key: '$key'")
            val result = Groups.selectAll().where { Groups.name eq key }.singleOrNull()
            logger.info("DEBUG: Query result: $result")
            result?.let { row ->
                Group(
                    id = row[Groups.id].value,
                    name = row[Groups.name],
                    memberUsernames = emptyList()
                )
            } ?: throw ResourceNotFoundException("Group", key)
        }
    }
    
    override suspend fun createGroup(name: String): ResultContainer<Group> = safeDbQuery {
        val existing = Groups.selectAll().where { Groups.name eq name }.singleOrNull()
        if (existing != null) {
            throw DuplicateResourceException("Group", name)
        }
        
        val insertedId = Groups.insert {
            it[Groups.name] = name
        } get Groups.id
        
        Group(
            id = insertedId.value,
            name = name,
            memberUsernames = emptyList()
        )
    }
    
    override suspend fun deleteGroup(key: String): ResultContainer<Unit> = safeDbQuery {
        val deletedCount = Groups.deleteWhere { Groups.name eq key }
        if (deletedCount == 0) {
            throw ResourceNotFoundException("Group", key)
        }
    }
}
