package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.presentation.controller.MembersController
import org.slf4j.LoggerFactory

class MembersCommand(
    private val membersController: MembersController
) {
    private val logger = LoggerFactory.getLogger(MembersCommand::class.java)

    suspend operator fun invoke(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return
        
        logger.info("Members command invoked - chatId: {}, userId: {}, username: {}", 
            chatId, user.id, user.username)
        
        val member = Member(
            id = 0,
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName ?: "Unknown",
            joinedAt = null,
        )
        
        val response = membersController.getMembers(member)
        
        logger.debug("Members response generated for userId: {} in chatId: {}", user.id, chatId)
        
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = response,
            parseMode = ParseMode.HTML
        )
        
        logger.debug("Members message sent to chatId: {}", chatId)
    }
}
