package com.ua.astrumon.presentation.controller

import com.ua.astrumon.common.exception.BusinessException
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.MemberService

class GroupController(
    private val groupService: GroupService,
    private val memberService: MemberService,
    private val autoRegisterService: AutoRegisterService
) {

    suspend fun getGroups(member: Member): String {
        autoRegisterService.ensureUserRegistered(
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )

        val result = groupService.getAllGroupsWithMembers().fold(
            onSuccess = { groups ->
                if (groups.isEmpty()) {
                    "<b>Немає груп</b>. Створи: /newgroup &lt;назва&gt;"
                } else {
                    val lines = mutableListOf("📋 <b>Групи:</b>")
                    groups.forEach { group ->
                        val names = if (group.members.isNotEmpty()) {
                            group.members.map { "@$it" }
                        } else {
                            listOf("—")
                        }
                        lines.add("• <b>${group.name}</b> (/ping ${group.key}): ${names.joinToString(", ")}")
                    }
                    lines.joinToString("\n")
                }
            },
            onFailure = {
                "❌ Помилка завантаження груп: ${it.userMessage}"
            }
        )
        return result
    }

    suspend fun createGroup(userId: Long, args: List<String>, adminIds: Set<Long>): String {
        if (userId !in adminIds) {
            return "🚫 Лише адміни."
        }

        if (args.isEmpty()) {
            return "Не правильно використовуєш команду, спробуй: /newgroup &lt;назва&gt;"
        }

        val name = args[0].lowercase()

        return groupService.createGroup(name).fold(
            onSuccess = {
                "✅ Група <b>$name</b> створена!\nВиклик: /ping $name"
            },
            onFailure = { exception ->
                when (exception) {
                    is DuplicateResourceException -> "⚠️ Група <b>$name</b> вже існує."
                    else -> "❌ Помилка створення групи: ${exception.userMessage}"
                }
            }
        )
    }

    suspend fun deleteGroup(userId: Long, args: List<String>, adminIds: Set<Long>): String {
        if (userId !in adminIds) {
            return "🚫 Лише адміни."
        }

        if (args.isEmpty()) {
            return "Використання: /delgroup &lt;назва&gt;"
        }

        val key = args[0].lowercase()

        return groupService.getGroupByKey(key).flatMap { group ->
            groupService.deleteGroup(key).map { group.name }
        }.fold(
            onSuccess = { groupName ->
                "🗑 Група <b>$groupName</b> видалена."
            },
            onFailure = { exception ->
                when (exception) {
                    is ResourceNotFoundException -> "❌ Групу $key не знайдено."
                    else -> "❌ Помилка видалення групи: ${exception.userMessage}"
                }
            }
        )
    }

    suspend fun addUserToGroup(userId: Long, args: List<String>, adminIds: Set<Long>): String {
        if (userId !in adminIds) {
            return "🚫 Лише адміни."
        }

        if (args.size < 2) {
            return "Використання: /addtogroup &lt;назва&gt; @username"
        }

        val key = args[0].lowercase()
        val username = args[1].removePrefix("@")

        return groupService.getGroupByKey(key).flatMap { group ->
            groupService.addMemberToGroup(key, username).map { group }
        }.fold(
            onSuccess = { group ->
                "✅ <b>$username</b> додано до <b>${group.name}</b>."
            },
            onFailure = { exception ->
                when (exception) {
                    is ResourceNotFoundException -> "❌ Групу $key не знайдено."
                    else -> "❌ Помилка додавання до групи: ${exception.userMessage}"
                }
            }
        )
    }

    suspend fun removeUserFromGroup(userId: Long, args: List<String>, adminIds: Set<Long>): String {
        if (userId !in adminIds) {
            return "🚫 Лише адміни."
        }

        if (args.size < 2) {
            return "Використання: /removefromgroup &lt;назва&gt; @username"
        }

        val key = args[0].lowercase()
        val username = args[1].removePrefix("@")

        return groupService.getGroupByKey(key).flatMap { group ->
            groupService.removeMemberFromGroup(key, username).map { group }
        }.fold(
            onSuccess = { group ->
                "✅ <b>$username</b> видалено з <b>${group.name}</b>."
            },
            onFailure = { exception ->
                when (exception) {
                    is ResourceNotFoundException -> "❌ Групу $key не знайдено."
                    is BusinessException -> "⚠️ $username не знайдено в групі."
                    else -> "❌ Помилка видалення з групи: ${exception.userMessage}"
                }
            }
        )
    }
}
