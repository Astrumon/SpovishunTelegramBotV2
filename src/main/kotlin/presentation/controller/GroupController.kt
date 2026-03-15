package com.ua.astrumon.presentation.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.ua.astrumon.common.exception.BusinessException
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.exception.ValidationException
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.MemberService
import org.slf4j.LoggerFactory

class GroupController(
    private val groupService: GroupService,
    private val memberService: MemberService,
    private val autoRegisterService: AutoRegisterService
) {
    private val logger = LoggerFactory.getLogger(GroupController::class.java)

    suspend fun getGroups(chatId: Long, member: Member): String {
        autoRegisterService.ensureUserRegistered(
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )

        val result = groupService.getAllGroupsWithMembers(chatId).fold(
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

    suspend fun getGroups(bot: Bot, chatId: Long, member: Member): String {
        autoRegisterService.ensureUserRegistered(
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )

        val result = groupService.getAllGroupsWithMembers(chatId).fold(
            onSuccess = { groups ->
                if (groups.isEmpty()) {
                    "<b>Немає груп</b>. Створи: /newgroup &lt;назва&gt;"
                } else {
                    val lines = mutableListOf("📋 <b>Групи:</b>")
                    groups.forEach { group ->
                        val names = if (group.members.isNotEmpty()) {
                            group.members.map { username ->
                                val memberRecord = memberService.getMemberByUsername(username)
                                val isAdmin = memberRecord.fold(
                                    onSuccess = { member -> 
                                        isAdmin(bot, chatId, member.userId)
                                    },
                                    onFailure = { false }
                                )
                                "@$username${if (isAdmin) " 🔐" else ""}"
                            }
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

    suspend fun createGroup(bot: Bot, chatId: Long, userId: Long, args: List<String>): String {
        if (!isAdmin(bot, chatId, userId)) return "🚫 Лише адміни."

        if (args.isEmpty()) {
            return "Не правильно використовуєш команду, спробуй: /newgroup &lt;назва&gt;"
        }

        val name = args[0].lowercase()

        return groupService.createGroup(chatId, name).fold(
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

    suspend fun deleteGroup(bot: Bot, chatId: Long, userId: Long, args: List<String>): String {
        if (!isAdmin(bot, chatId, userId)) return "🚫 Лише адміни."

        if (args.isEmpty()) {
            return "Використання: /delgroup &lt;назва&gt;"
        }

        val key = args[0].lowercase()

        return groupService.getGroupByKey(chatId, key).flatMap { group ->
            groupService.deleteGroup(chatId, key).map { group.name }
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

    suspend fun addUserToGroup(bot: Bot, chatId: Long, userId: Long, args: List<String>): String {
        if (!isAdmin(bot, chatId, userId)) return "🚫 Лише адміни."

        if (args.size < 2) {
            return "Використання: /addtogroup &lt;назва&gt; @username"
        }

        val key = args[0].lowercase()
        val username = args[1].removePrefix("@")
        logger.info("Processing addUserToGroup with key: '$key' and username: '$username'")

        return groupService.getGroupByKey(chatId, key).flatMap { group ->
            groupService.addMemberToGroup(chatId, key, username).map { group }
        }.fold(
            onSuccess = { group ->
                "✅ <b>$username</b> додано до <b>${group.name}</b>."
            },
            onFailure = { exception ->
                when (exception) {
                    is ValidationException -> "❌ Неможливо додати користувача @$username. Перевірте чи існує такий користувач"
                    is ResourceNotFoundException -> {
                        // Check if it's group or member not found by examining the exception message
                        if (exception.message?.contains("Group") == true) {
                            "❌ Групу $key не знайдено."
                        } else {
                            "❌ Користувача @$username не знайдено."
                        }
                    }

                    is DuplicateResourceException -> "⚠️ Користувач @$username вже в групі <b>$key</b>."
                    else -> "❌ Помилка додавання до групи: ${exception.userMessage}"
                }
            }
        )
    }

    suspend fun removeUserFromGroup(bot: Bot, chatId: Long, userId: Long, args: List<String>): String {
        if (!isAdmin(bot, chatId, userId)) return "🚫 Лише адміни."

        if (args.size < 2) {
            return "Використання: /removefromgroup &lt;назва&gt; @username"
        }

        val key = args[0].lowercase()
        val username = args[1].removePrefix("@")

        return groupService.getGroupByKey(chatId, key).flatMap { group ->
            groupService.removeMemberFromGroup(chatId, key, username).map { group }
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

    private fun isAdmin(bot: Bot, chatId: Long, userId: Long): Boolean {
        val admins = bot.getChatAdministrators(ChatId.fromId(chatId))
        return admins.getOrNull()?.any { it.user.id == userId } == true
    }
}
