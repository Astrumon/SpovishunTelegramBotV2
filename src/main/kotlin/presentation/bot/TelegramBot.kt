package com.ua.astrumon.presentation.bot

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.ua.astrumon.presentation.bot.commands.GrantRoleCommand
import com.ua.astrumon.presentation.bot.commands.GroupCommand
import com.ua.astrumon.presentation.bot.commands.MembersCommand
import com.ua.astrumon.presentation.bot.commands.PingCommand
import com.ua.astrumon.presentation.bot.commands.RegisterCommand
import com.ua.astrumon.presentation.bot.commands.StartCommand
import com.ua.astrumon.presentation.bot.handler.MessageHandler
import org.slf4j.LoggerFactory


class TelegramBot(
    private val startCommand: StartCommand,
    private val registerCommand: RegisterCommand,
    private val pingCommand: PingCommand,
    private val groupCommand: GroupCommand,
    private val grantRoleCommand: GrantRoleCommand,
    private val membersCommand: MembersCommand,
    private val messageHandler: MessageHandler
) {
    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)

    fun create(token: String) = bot {
        this.token = token
        
        dispatch {
            command("start") {
                startCommand(bot, update)
            }
            
            command("register") {
                registerCommand(bot, update)
            }
            
            command("all") {
                pingCommand.pingAll(bot, update)
            }
            
            command("ping") {
                pingCommand.pingGroup(bot, update)
            }
            
            command("groups") {
                groupCommand.showGroups(bot, update)
            }
            
            command("members") {
                membersCommand(bot, update)
            }
            
            command("newgroup") {
                groupCommand.addNewGroup(bot, update)
            }
            
            command("delgroup") {
                groupCommand.deleteGroup(bot, update)
            }
            
            command("addtogroup") {
                groupCommand.addUserToGroup(bot, update)
            }
            
            command("removefromgroup") {
                groupCommand.removeUserFromGroup(bot, update)
            }

            command("grantrole") {
                grantRoleCommand(bot, update)
            }

            message(Filter.Text) {
                messageHandler.handleIncomingMessage(bot, update)
            }
        }
    }
    
    suspend fun startPolling(bot: com.github.kotlintelegrambot.Bot) {
        try {
            bot.startPolling()
            logger.info("Bot started successfully")
        } catch (e: Exception) {
            logger.error("Failed to start bot", e)
            throw e
        }
    }
}
