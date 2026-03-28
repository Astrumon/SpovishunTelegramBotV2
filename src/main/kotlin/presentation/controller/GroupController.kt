package com.ua.astrumon.presentation.controller

import com.github.kotlintelegrambot.Bot
import com.ua.astrumon.common.exception.BusinessException
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.exception.ValidationException
import com.ua.astrumon.domain.BotAdminUtils
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
import com.ua.astrumon.domain.model.badge
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.MemberService
import org.slf4j.LoggerFactory

class GroupController(
    private val groupService: GroupService,
    private val memberService: MemberService,
    private val autoRegisterService: AutoRegisterService,
    private val botAdminUtils: BotAdminUtils,
) {
    private val logger = LoggerFactory.getLogger(GroupController::class.java)

    suspend fun getGroups(bot: Bot, chatId: Long, member: Member): String {
        val userRole = botAdminUtils.getMemberRole(bot, chatId, member.userId)
        autoRegisterService.ensureUserRegistered(
            chatId = chatId,
            userId = member.userId,
            username = member.username,
            firstName = member.firstName,
            userRole = userRole
        )

        return groupService.getAllGroupsWithMembers(chatId).fold(
            onSuccess = { groups ->
                if (groups.isEmpty()) {
                    "<b>Немає груп</b>. Створи: /newgroup &lt;назва&gt;"
                } else {
                    val lines = mutableListOf("📋 <b>Групи:</b>")
                    groups.forEach { group ->
                        val names = if (group.members.isNotEmpty()) {
                            group.members.map { username ->
                                val badge = memberService.getMemberByUsername(username)
                                    .fold(onSuccess = { it.role.badge() }, onFailure = { "" })
                                "@$username$badge"
                            }
                        } else {
                            listOf("—")
                        }
                        lines.add("• <b>${group.name}</b> (/ping ${group.key}): ${names.joinToString(", ")}")
                    }
                    lines.joinToString("\n")
                }
            },
            onFailure = { "❌ Помилка завантаження груп: ${it.userMessage}" }
        )
    }

    suspend fun createGroup(chatId: Long, userId: Long, args: List<String>): String {
        if (!hasModerationAccess(chatId, userId)) return "🚫 Лише адміни та модератори."

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

    suspend fun deleteGroup(chatId: Long, userId: Long, args: List<String>): String {
        if (!hasModerationAccess(chatId, userId)) return "🚫 Лише адміни та модератори."

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

    suspend fun addUserToGroup(chatId: Long, userId: Long, args: List<String>): String {
        if (!hasModerationAccess(chatId, userId)) return "🚫 Лише адміни та модератори."

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
                        if (exception.resource == "Group") {
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

    suspend fun removeUserFromGroup(chatId: Long, userId: Long, args: List<String>): String {
        if (!hasModerationAccess(chatId, userId)) return "🚫 Лише адміни та модератори."

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

    suspend fun grantRole(chatId: Long, userId: Long, args: List<String>): String {
        if (!hasAdminAccess(chatId, userId)) return "🚫 Лише адміни можуть призначати ролі."

        if (args.size < 2) return "Використання: /grantrole @username moderator|admin|member"

        val targetUsername = args[0].removePrefix("@")
        val roleArg = args[1].uppercase()

        val role = runCatching { MemberRole.valueOf(roleArg) }.getOrNull()
            ?: return "❌ Невідома роль: ${args[1]}. Доступні: moderator, admin, member"

        return memberService.getMemberByUsername(targetUsername)
            .flatMap { targetMember -> memberService.setMemberRole(chatId, targetMember.userId, role) }
            .fold(
                onSuccess = { "✅ @$targetUsername отримав роль ${role.name.lowercase()}." },
                onFailure = { exception ->
                    when (exception) {
                        is ResourceNotFoundException -> "❌ Користувача @$targetUsername не знайдено."
                        else -> "❌ Помилка: ${exception.userMessage}"
                    }
                }
            )
    }

    private suspend fun hasModerationAccess(chatId: Long, userId: Long): Boolean {
        return memberService.getMemberByChatAndUserId(chatId, userId)
            .fold(onSuccess = { it.role >= MemberRole.MODERATOR }, onFailure = { false })
    }

    private suspend fun hasAdminAccess(chatId: Long, userId: Long): Boolean {
        return memberService.getMemberByChatAndUserId(chatId, userId)
            .fold(onSuccess = { it.role == MemberRole.ADMIN }, onFailure = { false })
    }
}
