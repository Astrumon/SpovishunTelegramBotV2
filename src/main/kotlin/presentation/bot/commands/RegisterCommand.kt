package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.MemberService

class RegisterCommand(
    private val memberService: MemberService
) {
    
    suspend operator fun invoke(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return

        val member = Member(
            id = 0, // Will be set by database
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName
        )
        
        val result = memberService.createMember(
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )
        
        val responseText = if (result.isSuccess) {
            "✅ ${user.firstName}, ви успішно зареєстровані в системі!"
        } else {
            "ℹ️ ${user.firstName}, ви вже зареєстровані в системі."
        }
        
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = responseText,
            parseMode = ParseMode.HTML
        )
    }
}
