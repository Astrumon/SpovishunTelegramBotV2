package com.ua.astrumon.presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.ua.astrumon.domain.service.MemberService
import com.ua.astrumon.domain.service.AutoRegisterService

class StartCommand(
    private val memberService: MemberService,
    private val autoRegisterService: AutoRegisterService
) {

    suspend operator fun invoke(bot: Bot, update: Update) {
        val chatId = update.message?.chat?.id ?: return
        val user = update.message?.from

        addAllChatMembers(bot, chatId, user)
        
        val text = """
            👋 <b>Spovishun активний!</b>
            
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
    }
    
    private suspend fun addAllChatMembers(bot: Bot, chatId: Long, triggerUser: com.github.kotlintelegrambot.entities.User?) {
        try {
            // Get chat information to determine chat type
            val chatResponse = bot.getChat(ChatId.fromId(chatId))

            if (chatResponse.isSuccess && chatResponse.getOrNull() != null) {
                val chat = chatResponse.get()
                
                when {
                    chat.type == "group" -> {
                        addChatAdministrators(bot, chatId)
                        sendRegistrationInvitation(bot, chatId)
                    }

                    chat.type == "supergroup" -> {
                        addChatAdministrators(bot, chatId)
                    }
                }
            }

            if (triggerUser != null) {
                addMemberToDatabase(triggerUser.id, triggerUser.username, triggerUser.firstName)
            }
            
        } catch (e: Exception) {
            println("Error adding chat members: ${e.message}")
        }
    }
    
    private suspend fun addChatAdministrators(bot: Bot, chatId: Long) {
        try {
            val adminsResponse = bot.getChatAdministrators(ChatId.fromId(chatId))
            if (adminsResponse.isSuccess && adminsResponse.getOrNull() != null) {
                adminsResponse.get().forEach { admin ->
                    addMemberToDatabase(admin.user.id, admin.user.username, admin.user.firstName)
                }
            }
        } catch (e: Exception) {
            println("Error adding chat administrators: ${e.message}")
        }
    }
    
    private fun sendRegistrationInvitation(bot: Bot, chatId: Long) {
        try {
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
            println("Error sending registration invitation: ${e.message}")
        }
    }
    
    private suspend fun addMemberToDatabase(userId: Long, username: String?, firstName: String) {
        val result = autoRegisterService.ensureUserRegistered(
            userId = userId,
            username = username ?: "user_${userId}",
            firstName = firstName
        )
        
        if (result.isFailure) {
            println("Failed to register member ${userId}: ${result.exceptionOrNull()?.message}")
        }
    }
}
