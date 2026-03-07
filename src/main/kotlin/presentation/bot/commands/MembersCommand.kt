package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.presentation.controller.MembersController

class MembersCommand(
    private val membersController: MembersController
) {

    suspend operator fun invoke(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return
        
        val member = Member(
            id = 0,
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName ?: "Unknown"
        )
        
        val response = membersController.getMembers(member)
        
        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = response,
            parseMode = ParseMode.HTML
        )
    }
}
