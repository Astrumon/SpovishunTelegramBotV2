package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.common.util.VersionInfo
import org.slf4j.LoggerFactory

class StartCommand(private val autoRegisterService: AutoRegisterService) {
    private val logger = LoggerFactory.getLogger(StartCommand::class.java)

    suspend operator fun invoke(bot: Bot, update: Update) {
        val chatId = update.message?.chat?.id ?: return
        val user = update.message?.from

        logger.info(
            "Start command invoked - chatId: {}, userId: {}, username: {}, firstName: {}",
            chatId, user?.id, user?.username, user?.firstName
        )

        addAllChatMembers(bot, chatId, user)

        logger.info("Sending welcome message to chatId: {}", chatId)

        val text = """
            👋 <b>Spovishun активний!</b> ${VersionInfo.getFullVersion()}
            
            📋 <b>Реєстрація:</b>
            • /register — зареєструватися в системі
            • Або просто напишіть будь-яке повідомлення
            
            Команди:
            • /all — пінгнути всіх
            • /ping &lt;група&gt; [текст] — пінгнути групу
            • /groups — список груп
            • /members — список учасників
            
            🔐 <b>Адмін:</b>
            • /newgroup &lt;ключ&gt; &lt;назва&gt; — створити групу
            • /delgroup &lt;ключ&gt; — видалити групу
            • /addtogroup &lt;ключ&gt; @user — додати до групи
            • /removefromgroup &lt;ключ&gt; @user — видалити з групи
        """.trimIndent()

        bot.sendMessage(
            chatId = ChatId.fromId(chatId),
            text = text,
            parseMode = ParseMode.HTML
        )

        logger.debug("Welcome message sent successfully to chatId: {}", chatId)
    }

    private suspend fun addAllChatMembers(
        bot: Bot,
        chatId: Long,
        triggerUser: com.github.kotlintelegrambot.entities.User?
    ) {
        try {
            logger.debug("Adding chat members for chatId: {}, triggerUserId: {}", chatId, triggerUser?.id)
            // Get chat information to determine chat type
            val chatResponse = bot.getChat(ChatId.fromId(chatId))

            if (chatResponse.isSuccess && chatResponse.getOrNull() != null) {
                val chat = chatResponse.get()

                when {
                    chat.type == "group" -> {
                        logger.debug("Processing group chat: {}", chatId)
                        addChatAdministrators(bot, chatId)
                        sendRegistrationInvitation(bot, chatId)
                    }

                    chat.type == "supergroup" -> {
                        logger.debug("Processing supergroup chat: {}", chatId)
                        addChatAdministrators(bot, chatId)
                    }
                }
            }

            if (triggerUser != null) {
                addMemberToDatabase(
                    chatId = chatId,
                    userId = triggerUser.id,
                    username = triggerUser.username,
                    firstName = triggerUser.firstName
                )
            }

        } catch (e: Exception) {
            logger.error("Error adding chat members for chatId: {}", chatId, e)
        }
    }

    private suspend fun addChatAdministrators(bot: Bot, chatId: Long) {
        try {
            logger.debug("Fetching chat administrators for chatId: {}", chatId)
            val adminsResponse = bot.getChatAdministrators(ChatId.fromId(chatId))
            if (adminsResponse.isSuccess && adminsResponse.getOrNull() != null) {
                val admins = adminsResponse.get()
                logger.debug("Found {} administrators for chatId: {}", admins.size, chatId)
                admins.forEach { admin ->
                    logger.debug(
                        "Adding admin to database: userId: {}, username: {}",
                        admin.user.id, admin.user.username
                    )
                    addMemberToDatabase(
                        userId = admin.user.id,
                        chatId = chatId,
                        username = admin.user.username,
                        firstName = admin.user.firstName
                    )
                }
            } else {
                logger.warn("Failed to get chat administrators for chatId: {}", chatId)
            }
        } catch (e: Exception) {
            logger.error("Error adding chat administrators for chatId: {}", chatId, e)
        }
    }

    private fun sendRegistrationInvitation(bot: Bot, chatId: Long) {
        try {
            logger.info("Sending registration invitation to chatId: {}", chatId)
            val invitationText = """
                📋 <b>Реєстрація учасників</b>
                
                Я додав адміністраторів та користувачів з останніх повідомлень до бази даних.
                
                Якщо вас ще немає в системі, будь ласка, напишіть будь-яке повідомлення (наприклад, <code>/register</code>), щоб я міг вас зареєструвати.
                
                Це потрібно для того, щоб ви могли користуватися всіма функціями бота.
            """.trimIndent()

            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = invitationText,
                parseMode = ParseMode.HTML
            )
        } catch (e: Exception) {
            logger.error("Error sending registration invitation to chatId: {}", chatId, e)
        }
    }

    private suspend fun addMemberToDatabase(chatId: Long, userId: Long, username: String?, firstName: String) {
        val sanitizedUsername = sanitizeUsername(username, userId)
        logger.debug(
            "Attempting to register member: userId: {}, username: {}, firstName: {}",
            userId, sanitizedUsername, firstName
        )
        val result = autoRegisterService.ensureUserRegistered(
            userId = userId,
            chatId = chatId,
            username = sanitizedUsername,
            firstName = firstName
        )

        if (result.isFailure) {
            logger.error("Failed to register member {}: {}", userId, result.exceptionOrNull()?.message)
        } else {
            logger.debug("Successfully registered member: userId: {}", userId)
        }
    }

    private fun sanitizeUsername(username: String?, userId: Long): String {
        if (username.isNullOrBlank()) {
            return "user_${userId}"
        }

        val sanitized = username
            .trim()
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .take(32)

        return sanitized.ifEmpty { "user_${userId}" }
    }
}
