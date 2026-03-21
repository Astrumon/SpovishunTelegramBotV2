package com.ua.astrumon.presentation.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.MemberService

class MembersController(
    private val memberService: MemberService,
    private val autoRegisterService: AutoRegisterService
) {

    suspend fun getMembers(member: Member): String {
        autoRegisterService.ensureUserRegistered(
            chatId = member.chatId,
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )

        val result = memberService.getAllMembers().fold(
            onSuccess = { members ->
                if (members.isEmpty()) {
                    "📋 <b>Немає зареєстрованих учасників</b>.\n\nНапиши будь-яке повідомлення, щоб зареєструватися!"
                } else {
                    val lines = mutableListOf("📋 <b>Зареєстровані учасники:</b>")
                    members.forEach { member ->
                        val username = if (member.username.startsWith("user_")) {
                            member.firstName
                        } else {
                            "@${member.username}"
                        }
                        lines.add("• $username")
                    }
                    lines.joinToString("\n") + "\n\n📝 Всього: ${members.size} учасників"
                }
            },
            onFailure = {
                "❌ Помилка завантаження учасників: ${it.userMessage}"
            }
        )
        return result
    }

    suspend fun getMembers(bot: Bot, chatId: Long, member: Member): String {
        autoRegisterService.ensureUserRegistered(
            chatId = chatId,
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )

        val result = memberService.getAllMembers().fold(
            onSuccess = { members ->
                if (members.isEmpty()) {
                    "📋 <b>Немає зареєстрованих учасників</b>.\n\nНапиши будь-яке повідомлення, щоб зареєструватися!"
                } else {
                    val lines = mutableListOf("📋 <b>Зареєстровані учасники:</b>")
                    members.forEach { member ->
                        val username = if (member.username.startsWith("user_")) {
                            member.firstName
                        } else {
                            val isAdmin = isAdmin(bot, chatId, member.userId)
                            "@${member.username}${if (isAdmin) " 🔐" else ""}"
                        }
                        lines.add("• $username")
                    }
                    lines.joinToString("\n") + "\n\n📝 Всього: ${members.size} учасників"
                }
            },
            onFailure = {
                "❌ Помилка завантаження учасників: ${it.userMessage}"
            }
        )
        return result
    }

    private fun isAdmin(bot: Bot, chatId: Long, userId: Long): Boolean {
        val admins = bot.getChatAdministrators(ChatId.fromId(chatId))
        return admins.getOrNull()?.any { it.user.id == userId } == true
    }
}
