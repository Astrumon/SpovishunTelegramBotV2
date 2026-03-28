package com.ua.astrumon.di

import com.ua.astrumon.config.AppConfig
import com.ua.astrumon.domain.BotAdminUtils
import com.ua.astrumon.presentation.bot.TelegramBot
import com.ua.astrumon.presentation.bot.commands.GrantRoleCommand
import com.ua.astrumon.presentation.bot.commands.GroupCommand
import com.ua.astrumon.presentation.bot.commands.MembersCommand
import com.ua.astrumon.presentation.bot.commands.PingCommand
import com.ua.astrumon.presentation.bot.commands.RegisterCommand
import com.ua.astrumon.presentation.bot.commands.StartCommand
import com.ua.astrumon.presentation.bot.handler.MessageHandler
import com.ua.astrumon.presentation.controller.GroupController
import com.ua.astrumon.presentation.controller.MembersController
import org.koin.dsl.module

val presentationModule = module {
    // Controllers
    single { GroupController(get(), get(), get(), get()) }
    single { MembersController(get(), get(), get()) }

    // Bot components
    single { TelegramBot(get(), get(), get(), get(), get(), get(), get()) }
    single { MessageHandler(get(), get()) }
    single { BotAdminUtils() }

    // Commands
    single { StartCommand(get(), get()) }
    single { RegisterCommand(get(), get()) }
    single { GroupCommand(get()) }
    single { GrantRoleCommand(get()) }
    single { PingCommand(get(), get(), get(), get()) }
    single { MembersCommand(get()) }
}
