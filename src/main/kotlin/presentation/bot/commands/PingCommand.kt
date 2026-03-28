package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.presentation.util.BotAdminUtils
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.MemberService
import org.slf4j.LoggerFactory

class PingCommand(
    private val memberService: MemberService,
    private val groupService: GroupService,
    private val autoRegisterService: AutoRegisterService,
    private val botAdminUtils: BotAdminUtils,
) {
    private val logger = LoggerFactory.getLogger(PingCommand::class.java)

    suspend fun pingAll(bot: Bot, update: Update) {
        val chatId = update.message?.chat?.id ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        val user = update.message?.from
        
        logger.info("PingAll command invoked - chatId: {}, userId: {}, args: {}", 
            chatId, user?.id, args)

        ensureUserRegistered(bot, update)
        
        logger.debug("Fetching all members for pingAll in chatId: {}", chatId)

        // Get all members
        val membersResult = memberService.getAllMembers()
        if (membersResult.isFailure) {
            logger.error("Failed to get all members for pingAll: {}", membersResult.exceptionOrNull()?.message)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Помилка завантаження учасників.",
                parseMode = ParseMode.HTML
            )
            return
        }

        val members = membersResult.getOrNull() ?: emptyList()
        logger.debug("Found {} members for pingAll", members.size)
        if (members.isEmpty()) {
            logger.info("No registered members found for pingAll in chatId: {}", chatId)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Немає зареєстрованих учасників.",
                parseMode = ParseMode.HTML
            )
            return
        }

        val extra = args.joinToString(" ")
        val crabs = "🗿".repeat(members.size)
        val header = if (extra.isNotEmpty()) "📢 $crabs $extra" else "📢 $crabs"

        sendPing(bot, chatId, header, members.map { it.username })
        logger.info("PingAll sent to {} members in chatId: {}", members.size, chatId)
    }

    suspend fun pingGroup(bot: Bot, update: Update) {
        val chatId = update.message?.chat?.id ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()
        val user = update.message?.from
        
        logger.info("PingGroup command invoked - chatId: {}, userId: {}, args: {}", 
            chatId, user?.id, args)

        ensureUserRegistered(bot, update)

        if (args.isEmpty()) {
            logger.warn("PingGroup called without group key in chatId: {}", chatId)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Використання: /ping &lt;група&gt; [текст]",
                parseMode = ParseMode.HTML
            )
            return
        }

        val groupKey = args[0].lowercase()
        logger.debug("Looking for group with key: {} in chatId: {}", groupKey, chatId)
        val groupResult = groupService.getGroupByKey(chatId, groupKey)

        if (groupResult.isFailure) {
            logger.warn("Group '{}' not found for pingGroup in chatId: {}", groupKey, chatId)
            val allGroupsResult = groupService.getAllGroupsWithMembers(chatId)
            val availableKeys = if (allGroupsResult.isSuccess) {
                allGroupsResult.getOrNull()?.joinToString(", ") { it.key } ?: "—"
            } else {
                "—"
            }

            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Групу <b>$groupKey</b> не знайдено.\nДоступні: $availableKeys",
                parseMode = ParseMode.HTML
            )
            return
        }

        val group = groupResult.getOrNull()
        
        // Validate that group members actually exist in the member database
        val validMembers = mutableListOf<String>()
        if (group?.members?.isNotEmpty() == true) {
            logger.debug("Group '{}' has {} members, validating against member database", groupKey, group.members.size)
            
            for (username in group.members) {
                val memberResult = memberService.getMemberByUsername(username)
                if (memberResult.isSuccess) {
                    validMembers.add(username)
                    logger.debug("Member '{}' found in database", username)
                } else {
                    logger.warn("Member '{}' from group '{}' not found in member database", username, groupKey)
                }
            }
        }
        
        if (validMembers.isEmpty()) {
            logger.info("No valid members to ping in group '{}' for chatId: {} (original members: {})", 
                groupKey, chatId, group?.members?.size ?: 0)
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Немає кого пінгувати.",
                parseMode = ParseMode.HTML
            )
            return
        }

        val extra = args.drop(1).joinToString(" ")
        val crabs = "🦞".repeat(validMembers.size)
        val header = if (extra.isNotEmpty()) "📣 $crabs $extra" else "📣 $crabs"
        val mentions = validMembers.joinToString(" ") { "@$it" }
        val text = "$header\n\n$mentions"

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.HTML
        )
        
        logger.info("PingGroup sent to {} valid members in group '{}' for chatId: {} (original members: {})", 
            validMembers.size, groupKey, chatId, group?.members?.size ?: 0)
    }

    private suspend fun sendPing(bot: Bot, chatId: Long, header: String, usernames: List<String>) {
        logger.debug("Sending ping to {} users in chatId: {}", usernames.size, chatId)
        val mentions = usernames.joinToString(" ") { "@$it" }
        val text = "$header\n\n$mentions"

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.HTML
        )
    }

    private suspend fun ensureUserRegistered(bot: Bot, update: Update) {
        val user = update.message?.from ?: return
        val chatId = update.message?.chat?.id ?: return
        val userRole = botAdminUtils.getMemberRole(bot, chatId, user.id)
        
        logger.debug("Ensuring user is registered - userId: {}, username: {}", 
            user.id, user.username)

        autoRegisterService.ensureUserRegistered(
            userId = user.id,
            chatId = chatId,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName,
            userRole = userRole
        )
    }
}
