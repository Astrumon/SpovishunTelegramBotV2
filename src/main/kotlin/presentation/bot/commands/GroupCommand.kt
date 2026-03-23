package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.presentation.controller.GroupController
import org.slf4j.LoggerFactory


class GroupCommand(
    private val groupController: GroupController,
    private val adminIds: Set<Long>
) {
    private val logger = LoggerFactory.getLogger(GroupCommand::class.java)
    
    suspend fun showGroups(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        
        val member = Member(
            id = 0,
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName ?: "Unknown"
        )
        
        val response = groupController.getGroups(member)
        
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )
    }
    
    suspend fun addNewGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        
        val response = groupController.createGroup(
            userId = user.id,
            args = args,
            adminIds = adminIds
        )
        logger.info("DEV: addNewGroup response=$response")
        
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )
    }
    
    suspend fun deleteGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        
        val response = groupController.deleteGroup(
            userId = user.id,
            args = args,
            adminIds = adminIds
        )
        
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )
    }
    
    suspend fun addUserToGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        
        val response = groupController.addUserToGroup(
            userId = user.id,
            args = args,
            adminIds = adminIds
        )
        
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )
    }
    
    suspend fun removeUserFromGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        
        val response = groupController.removeUserFromGroup(
            userId = user.id,
            args = args,
            adminIds = adminIds
        )
        
        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )
    }
}
