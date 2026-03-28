package com.ua.astrumon.presentation.controller

import com.github.kotlintelegrambot.Bot
import com.ua.astrumon.domain.BotAdminUtils
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.badge
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.MemberService

class MembersController(
    private val memberService: MemberService,
    private val autoRegisterService: AutoRegisterService,
    private val botAdminUtils: BotAdminUtils
) {

    suspend fun getMembers(bot: Bot, chatId: Long, member: Member): String {
        val userRole = botAdminUtils.getMemberRole(bot, chatId, member.id)
        autoRegisterService.ensureUserRegistered(
            chatId = chatId,
            userId = member.userId,
            username = member.username,
            firstName = member.firstName,
            userRole = userRole
        )

        return memberService.getAllMembers().fold(
            onSuccess = { members ->
                if (members.isEmpty()) {
                    "📋 <b>Немає зареєстрованих учасників</b>.\n\nНапиши будь-яке повідомлення, щоб зареєструватися!"
                } else {
                    val lines = mutableListOf("📋 <b>Зареєстровані учасники:</b>")
                    members.forEach { m ->
                        val display = if (m.username.startsWith("user_")) {
                            m.firstName
                        } else {
                            "@${m.username}${m.role.badge()}"
                        }
                        lines.add("• $display")
                    }
                    lines.joinToString("\n") + "\n\n📝 Всього: ${members.size} учасників"
                }
            },
            onFailure = { "❌ Помилка завантаження учасників: ${it.userMessage}" }
        )
    }
}
