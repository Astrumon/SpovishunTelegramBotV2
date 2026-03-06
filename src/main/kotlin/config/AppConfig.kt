package org.example.config

import io.github.cdimascio.dotenv.dotenv

class AppConfig {
    private val env = dotenv()
    val botToken: String  = env["BOT_TOKEN"]
    val adminIds: List<Long> = env["ADMINS"].split(",").map { it.toLong() }

}