package com.ua.astrumon.domain.service

import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import org.slf4j.LoggerFactory

class AutoRegisterService(
    private val memberService: MemberService
) {
    private val logger = LoggerFactory.getLogger(AutoRegisterService::class.java)

    suspend fun ensureUserRegistered(userId: Long, username: String, firstName: String): ResultContainer<Member> {
        return try {
            val existing = memberService.getMemberByUsername(username)
            if (existing.isSuccess) {
                logger.debug("User $username already exists with ID: ${existing.getOrNull()?.id}")
                return existing
            }

            logger.info("Auto-registering new user: $username (ID: $userId)")
            memberService.createMember(userId, username, firstName).also { result ->
                result.fold(
                    onSuccess = { member -> logger.info("Successfully auto-registered user: $username with ID: ${member.id}") },
                    onFailure = { error -> logger.error("Failed to auto-register user: $username", error) }
                )
            }
        } catch (e: Exception) {
            logger.error("Unexpected error during auto-registration for user: $username", e)
            ResultContainer.failure(DatabaseException("Unexpected error during auto-registration", e))
        }
    }

    suspend fun isUserRegistered(username: String): Boolean {
        return memberService.getMemberByUsername(username).isSuccess
    }
}
