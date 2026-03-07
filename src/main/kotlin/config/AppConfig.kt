package com.ua.astrumon.config

import io.github.cdimascio.dotenv.dotenv

class AppConfig {
    private val env = dotenv()
    val telegramBotToken: String = env["TELEGRAM_BOT_TOKEN"]
    val telegramAdminIds: Set<Long> = env["ADMINS"].split(",").map { it.toLong() }.toSet()
}
