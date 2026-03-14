package com.ua.astrumon.data.memory.repository

import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.NotFoundException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Group
import com.ua.astrumon.domain.repository.GroupRepository
import org.slf4j.LoggerFactory

class GroupRepositoryMockImpl : GroupRepository {
    private val logger = LoggerFactory.getLogger(GroupRepositoryMockImpl::class.java)
    private val groups = mutableMapOf<String, Group>()
    private var nextId = 1L
    
    override suspend fun getAllGroups(): ResultContainer<List<Group>> {
        logger.info("DEV: Getting all groups. Total count: ${groups.size}")
        return ResultContainer.success(groups.values.toList())
    }
    
    override suspend fun findGroupByKey(key: String): ResultContainer<Group> {
        logger.info("DEV: Finding group by key: $key")
        return groups[key]?.let { ResultContainer.success(it) }
            ?: ResultContainer.failure(NotFoundException("Group not found: $key"))
    }
    
    override suspend fun createGroup(name: String): ResultContainer<Group> {
        logger.info("DEV: Creating group - name: $name")
        if (groups.containsKey(name)) {
            return ResultContainer.failure(DuplicateResourceException("Group", name))
        }
        
        val group = Group(
            id = nextId++,
            name = name,
            memberUsernames = emptyList()
        )
        groups[name] = group
        logger.info("DEV: Group created successfully: $group")
        return ResultContainer.success(group)
    }
    
    override suspend fun deleteGroup(key: String): ResultContainer<Unit> {
        logger.info("DEV: Deleting group: $key")
        return if (groups.remove(key) != null) {
            logger.info("DEV: Group deleted successfully: $key")
            ResultContainer.success(Unit)
        } else {
            ResultContainer.failure(NotFoundException("Group not found: $key"))
        }
    }
}
