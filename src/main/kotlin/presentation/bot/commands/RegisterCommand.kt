package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.MemberService
import org.slf4j.LoggerFactory

class RegisterCommand(
    private val memberService: MemberService
) {
    private val logger = LoggerFactory.getLogger(RegisterCommand::class.java)
    
    suspend operator fun invoke(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return
        
        logger.info("Register command invoked - chatId: {}, userId: {}, username: {}, firstName: {}", 
            chatId, user.id, user.username, user.firstName)

        val member = Member(
            id = 0, // Will be set by database
            chatId = chatId,
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName,
            joinedAt = null
        )
        
        val result = memberService.createMember(
            chatId = chatId,
            userId = member.userId,
            username = member.username,
            firstName = member.firstName
        )
        
        if (result.isSuccess) {
            logger.info("Successfully registered member: userId: {}, username: {}", user.id, user.username)
        } else {
            logger.info("Member already exists or registration failed: userId: {}, username: {}, error: {}", 
                user.id, user.username, result.exceptionOrNull()?.message)
        }
        
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
