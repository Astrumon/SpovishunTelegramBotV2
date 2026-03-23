package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.MemberService

class PingCommand(
    private val memberService: MemberService,
    private val groupService: GroupService,
    private val autoRegisterService: AutoRegisterService
) {

    suspend fun pingAll(bot: Bot, update: Update) {
        val chatId = update.message?.chat?.id ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()

        ensureUserRegistered(update)

        // Get all members
        val membersResult = memberService.getAllMembers()
        if (membersResult.isFailure) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Помилка завантаження учасників.",
                parseMode = ParseMode.HTML
            )
            return
        }

        val members = membersResult.getOrNull() ?: emptyList()
        if (members.isEmpty()) {
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
    }

    suspend fun pingGroup(bot: Bot, update: Update) {
        val chatId = update.message?.chat?.id ?: return
        val args = update.message?.text?.split(" ")?.drop(1) ?: emptyList()

        ensureUserRegistered(update)

        if (args.isEmpty()) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Використання: /ping &lt;група&gt; [текст]",
                parseMode = ParseMode.HTML
            )
            return
        }

        val groupKey = args[0].lowercase()
        val groupResult = groupService.getGroupByKey(groupKey)

        if (groupResult.isFailure) {
            val allGroupsResult = groupService.getAllGroupsWithMembers()
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
        if (group?.members.isNullOrEmpty()) {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "Немає кого пінгувати.",
                parseMode = ParseMode.HTML
            )
            return
        }

        val extra = args.drop(1).joinToString(" ")
        val crabs = "🦞".repeat(group.members.size)
        val header = if (extra.isNotEmpty()) "📣 $crabs $extra" else "📣 $crabs"
        val mentions = group.members.joinToString(" ") { "@$it" }
        val text = "$header\n\n$mentions"

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.HTML
        )
    }

    private suspend fun sendPing(bot: Bot, chatId: Long, header: String, usernames: List<String>) {
        val mentions = usernames.joinToString(" ") { "@$it" }
        val text = "$header\n\n$mentions"

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.HTML
        )
    }

    private suspend fun ensureUserRegistered(update: Update) {
        val user = update.message?.from ?: return

        autoRegisterService.ensureUserRegistered(
            userId = user.id,
            username = user.username ?: "user_${user.id}",
            firstName = user.firstName
        )
    }
}
