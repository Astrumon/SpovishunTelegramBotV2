package com.ua.astrumon.domain.service

import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.repository.MemberRepository

class MemberService(
    private val memberRepository: MemberRepository
) {

    suspend fun createMember(userId: Long, username: String, firstName: String): ResultContainer<Member> {
        return checkUsernameExists(username)
            .flatMap {
                memberRepository.save(userId = userId, username = username, firstName = firstName)
            }
    }

    suspend fun getMemberByUsername(username: String): ResultContainer<Member> {
        return memberRepository.findByUsername(username)
            .flatMap { member ->
                if (member != null) {
                    ResultContainer.success(member)
                } else {
                    ResultContainer.failure(ResourceNotFoundException("Member", username))
                }
            }
    }

    suspend fun updateMemberUsername(currentUsername: String, newUsername: String): ResultContainer<Member> {
        return getMemberByUsername(currentUsername)
            .flatMap { currentMember ->
                if (currentUsername == newUsername) {
                    ResultContainer.success(currentMember)
                } else {
                    checkUsernameExists(newUsername)
                        .flatMap {
                            memberRepository.save(
                                userId = currentMember.userId,
                                username = newUsername,
                                firstName = currentMember.firstName
                            )
                        }
                }
            }
    }

    suspend fun getAllMembers(): ResultContainer<List<Member>> {
        return memberRepository.findAll()
    }

    private suspend fun checkUsernameExists(username: String): ResultContainer<String> {
        return memberRepository.findByUsername(username)
            .flatMap { member ->
                if (member == null) {
                    ResultContainer.success(username)
                } else {
                    ResultContainer.failure(DuplicateResourceException("Member", username))
                }
            }
    }

}
