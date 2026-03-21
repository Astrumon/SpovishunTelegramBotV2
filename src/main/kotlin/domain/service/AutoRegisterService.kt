package com.ua.astrumon.domain.service

import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.exception.ValidationException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import org.slf4j.LoggerFactory

class AutoRegisterService(
    private val memberService: MemberService
) {
    private val logger = LoggerFactory.getLogger(AutoRegisterService::class.java)

    suspend fun ensureUserRegistered(chatId: Long, userId: Long, username: String, firstName: String): ResultContainer<Member> {
        // Validate userId
        if (userId == -1L) {
            logger.warn("Attempted to register user with invalid userId: -1, username: $username")
            return ResultContainer.failure(ValidationException("Cannot register user with invalid userId: -1"))
        }

        return try {
            memberService.getMemberByUsername(username)
                .fold(
                    onSuccess = { member ->
                        logger.debug("User $username already exists with ID: ${member.id}")
                        ResultContainer.success(member)
                    },
                    onFailure = { error ->
                        if (error is ResourceNotFoundException) {
                            logger.info("Auto-registering new user: $username (ID: $userId)")
                            memberService.createMember(chatId, userId, username, firstName)
                                .onSuccess { member ->
                                    logger.info("Successfully auto-registered user: $username with ID: ${member.id}")
                                }
                                .onFailure { createError ->
                                    logger.error("Failed to auto-register user: $username", createError)
                                }
                        } else {
                            logger.error("Member lookup failed for user: $username", error)
                            ResultContainer.failure(error)
                        }
                    }
                )
        } catch (e: Exception) {
            logger.error("Unexpected error during auto-registration for user: $username", e)
            ResultContainer.failure(DatabaseException("Unexpected error during auto-registration", e))
        }
    }

    suspend fun isUserRegistered(username: String): Boolean {
        return memberService.getMemberByUsername(username).isSuccess
    }
}
