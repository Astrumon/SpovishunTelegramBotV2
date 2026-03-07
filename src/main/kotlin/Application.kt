package com.ua.astrumon

import com.ua.astrumon.config.AppConfig
import com.ua.astrumon.di.configModule
import com.ua.astrumon.di.devRepositoryModule
import com.ua.astrumon.di.presentationModule
import com.ua.astrumon.di.prodRepositoryModule
import com.ua.astrumon.di.serviceModule
import com.ua.astrumon.presentation.bot.TelegramBot
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

object Application : KoinComponent {
    private val telegramBot: TelegramBot by inject()
    private val config: AppConfig by inject()
    
    suspend fun run() {
        initializeKoin()

        val bot = telegramBot.create(config.telegramBotToken)
        telegramBot.startPolling(bot)
    }

    fun initializeKoin() {
        val profile = System.getenv("PROFILE") ?: "dev"
        
        val repositoryModule = when (profile) {
            "prod" -> prodRepositoryModule
            else -> devRepositoryModule
        }

        println("WARN: profile=$profile")
        
        startKoin {
            modules(
                configModule,
                repositoryModule,
                serviceModule,
                presentationModule
            )
        }
    }
}


