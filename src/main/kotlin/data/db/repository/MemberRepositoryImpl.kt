package com.ua.astrumon.data.db.repository

import com.ua.astrumon.common.extension.safeDbQuery
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.data.db.table.Members
import com.ua.astrumon.data.mapper.toMember
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.repository.MemberRepository
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll

class MemberRepositoryImpl : MemberRepository {
    override suspend fun findByUsername(username: String): ResultContainer<Member?> =
        safeDbQuery {
            findMemberByUsername(username)
        }

    override suspend fun findByUserId(userId: Long): ResultContainer<Member?> =
        safeDbQuery {
            findMemberByUserId(userId)
        }

    override suspend fun save(userId: Long, username: String, firstName: String): ResultContainer<Member> =
        safeDbQuery {
            val existing = findMemberByUsername(username)
            if (existing != null) {
                throw DuplicateResourceException("Member", username)
            }
            
            Members.insertIgnore {
                it[this@insertIgnore.userId] = userId
                it[this@insertIgnore.username] = username
                it[this@insertIgnore.firstName] = firstName
            }

            findMemberByUsername(username) ?: throw ResourceNotFoundException("Member", username)
        }

    private fun findMemberByUsername(username: String): Member? {
        return findMemberBy { Members.username eq username }
    }

    private fun findMemberByUserId(userId: Long): Member? {
        return findMemberBy { Members.userId eq userId }
    }

    private fun findMemberBy(predicate: (Members) -> Op<Boolean>): Member? {
        return Members.selectAll()
            .where { predicate(Members) }
            .singleOrNull()?.toMember()
    }

    override suspend fun findAll(): ResultContainer<List<Member>> =
        safeDbQuery { Members.selectAll().map { it.toMember() } }
}
