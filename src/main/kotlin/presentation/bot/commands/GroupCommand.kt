package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.data.memory.repository.GroupMemberRepositoryMockImpl
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.presentation.controller.GroupController
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory


class GroupCommand(
    private val groupController: GroupController,
) {
    private val logger = LoggerFactory.getLogger(GroupCommand::class.java)

    suspend fun showGroups(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return

        logger.info(
            "Groups command invoked - chatId: {}, userId: {}, username: {}",
            chatId, user.id, user.username
        )

        val member = Member(
            id = 0,
            chatId = chatId,
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName ?: "Unknown",
            joinedAt = null,
        )

        val response = groupController.getGroups(bot, chatId, member)

        logger.debug("Groups response generated for userId: {} in chatId: {}", user.id, chatId)

        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )

        logger.debug("Groups message sent to chatId: {}", chatId)
    }

    suspend fun addNewGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        val chatId = update.message?.chat?.id ?: return

        logger.info(
            "NewGroup command invoked - chatId: {}, userId: {}, args: {}",
            chatId, user.id, args
        )

        val response = groupController.createGroup(
            bot = bot,
            chatId = chatId,
            userId = user.id,
            args = args,
        )
        logger.info("NewGroup response for userId: {} in chatId: {}: {}", user.id, chatId, response)

        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )
    }

    suspend fun deleteGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        val chatId = update.message?.chat?.id ?: return

        logger.info(
            "DeleteGroup command invoked - chatId: {}, userId: {}, args: {}",
            chatId, user.id, args
        )

        val response = groupController.deleteGroup(
            bot = bot,
            chatId = chatId,
            userId = user.id,
            args = args,
        )

        logger.info("DeleteGroup response for userId: {} in chatId: {}: {}", user.id, chatId, response)

        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )

        logger.debug("DeleteGroup message sent to chatId: {}", chatId)
    }

    suspend fun addUserToGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        val chatId = update.message?.chat?.id ?: return

        logger.info(
            "AddUserToGroup command invoked - chatId: {}, userId: {}, args: {}",
            chatId, user.id, args
        )

        val response = groupController.addUserToGroup(
            bot = bot,
            chatId,
            userId = user.id,
            args = args,
        )

        logger.info("AddUserToGroup response for userId: {} in chatId: {}: {}", user.id, chatId, response)

        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )

        logger.debug("AddUserToGroup message sent to chatId: {}", chatId)
    }

    suspend fun removeUserFromGroup(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        val chatId = update.message?.chat?.id ?: return

        logger.info(
            "RemoveUserFromGroup command invoked - chatId: {}, userId: {}, args: {}",
            chatId, user.id, args
        )

        val response = groupController.removeUserFromGroup(
            bot = bot,
            chatId = chatId,
            userId = user.id,
            args = args,
        )

        logger.info("RemoveUserFromGroup response for userId: {} in chatId: {}: {}", user.id, chatId, response)

        bot.sendMessage(
            chatId = ChatId.fromId(update.message!!.chat.id),
            text = response,
            parseMode = ParseMode.HTML
        )

        logger.debug("RemoveUserFromGroup message sent to chatId: {}", chatId)
    }
}
