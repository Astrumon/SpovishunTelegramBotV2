package com.ua.astrumon.domain.service

import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Group
import com.ua.astrumon.domain.repository.GroupMemberRepository
import com.ua.astrumon.domain.repository.GroupRepository
import org.slf4j.LoggerFactory

class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository
) {
    private val logger = LoggerFactory.getLogger(GroupService::class.java)

    suspend fun getAllGroupsWithMembers(): ResultContainer<List<GroupWithMembers>> {
        return groupRepository.getAllGroups().flatMap { groups ->
            val groupsWithMembers = groups.map { group ->
                groupMemberRepository.getGroupMembers(group.name).map { members ->
                    GroupWithMembers(
                        id = group.id,
                        key = group.name,
                        name = group.name,
                        members = members
                    )
                }
            }

            ResultContainer.catching {
                groupsWithMembers.map { it.getOrThrow() }
            }
        }
    }

    suspend fun createGroup(name: String): ResultContainer<Group> {
        return groupRepository.createGroup(name)
    }

    suspend fun deleteGroup(key: String): ResultContainer<Unit> {
        return groupRepository.deleteGroup(key)
    }

    suspend fun addMemberToGroup(key: String, username: String): ResultContainer<Unit> {
        return groupMemberRepository.addMemberToGroup(key, username)
    }

    suspend fun removeMemberFromGroup(key: String, username: String): ResultContainer<Unit> {
        return groupMemberRepository.removeMemberFromGroup(key, username)
    }

    suspend fun getGroupByKey(key: String): ResultContainer<GroupWithMembers> {
        logger.info("DEBUG: GroupService.getGroupByKey called with key: '$key'")
        return groupRepository.findGroupByKey(key).flatMap { group ->
            logger.info("DEBUG: GroupRepository found group: $group")
            groupMemberRepository.getGroupMembers(group.name).map { members ->
                GroupWithMembers(
                    id = group.id,
                    key = group.name,
                    name = group.name,
                    members = members
                )
            }
        }
    }
}

data class GroupWithMembers(
    val id: Long,
    val key: String,
    val name: String,
    val members: List<String>
)
