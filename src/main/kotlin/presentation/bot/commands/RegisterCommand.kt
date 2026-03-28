package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
import com.ua.astrumon.domain.service.MemberService
import com.ua.astrumon.domain.BotAdminUtils
import org.slf4j.LoggerFactory

class RegisterCommand(
    private val memberService: MemberService,
    private val botAdminUtils: BotAdminUtils
) {
    private val logger = LoggerFactory.getLogger(RegisterCommand::class.java)
    
    suspend operator fun invoke(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return
        
        logger.info("Register command invoked - chatId: {}, userId: {}, username: {}, firstName: {}", 
            chatId, user.id, user.username, user.firstName)

        val role = botAdminUtils.getMemberRole(bot, chatId, user.id)
        
        val result = memberService.createMember(
            chatId = chatId,
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName =user.firstName,
            role = role
        )
        
        if (result.isSuccess) {
            logger.info("Successfully registered member: userId: {}, username: {}, role: {}", 
                user.id, user.username, role)
        } else {
            logger.info("Member already exists or registration failed: userId: {}, username: {}, error: {}", 
                user.id, user.username, result.exceptionOrNull()?.message)
        }
        
        val responseText = if (result.isSuccess) {
            val roleText = if (role == MemberRole.ADMIN) " як адміністратор 🔐" else ""
            "✅ ${user.firstName}, ви успішно зареєстровані в системі$roleText!"
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
