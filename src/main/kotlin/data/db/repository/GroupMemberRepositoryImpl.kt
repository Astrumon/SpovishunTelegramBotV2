package com.ua.astrumon.data.db.repository

import com.ua.astrumon.common.extension.safeDbQuery
import com.ua.astrumon.common.exception.*
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.data.db.table.GroupMembers
import com.ua.astrumon.data.db.table.Groups
import com.ua.astrumon.data.db.table.Members
import com.ua.astrumon.domain.repository.GroupMemberRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class GroupMemberRepositoryImpl : GroupMemberRepository {
    
    override suspend fun addMemberToGroup(groupKey: String, username: String): ResultContainer<Unit> = safeDbQuery {
        val group = Groups.selectAll().where { Groups.name eq groupKey }.singleOrNull()
            ?: throw ResourceNotFoundException("Group", groupKey)

        val member = Members.selectAll().where { Members.username eq username }.singleOrNull()
            ?: throw ResourceNotFoundException("Member", username)

        val existing = GroupMembers.selectAll().where {
            (GroupMembers.group eq group[Groups.id]) and (GroupMembers.member eq member[Members.id])
        }.singleOrNull()
        
        if (existing != null) {
            return@safeDbQuery
        }

        GroupMembers.insert {
            it[GroupMembers.group] = group[Groups.id]
            it[GroupMembers.member] = member[Members.id]
        }
    }
    
    override suspend fun removeMemberFromGroup(groupKey: String, username: String): ResultContainer<Unit> = safeDbQuery {
        val group = Groups.selectAll().where { Groups.name eq groupKey }.singleOrNull()
            ?: throw ResourceNotFoundException("Group", groupKey)

        val member = Members.selectAll().where { Members.username eq username }.singleOrNull()
            ?: throw ResourceNotFoundException("Member", username)

        val deletedCount = GroupMembers.deleteWhere {
            (GroupMembers.group eq group[Groups.id]) and (GroupMembers.member eq member[Members.id])
        }
        
        if (deletedCount == 0) {
            throw BusinessException("Member $username is not in group $groupKey")
        }
    }
    
    override suspend fun getGroupMembers(groupKey: String): ResultContainer<List<String>> = safeDbQuery {
        val group = Groups.selectAll().where { Groups.name eq groupKey }.singleOrNull()
            ?: throw ResourceNotFoundException("Group", groupKey)

        GroupMembers
            .innerJoin(Members)
            .selectAll()
            .where { GroupMembers.group eq group[Groups.id] }
            .map { row -> row[Members.username] }
    }
}
