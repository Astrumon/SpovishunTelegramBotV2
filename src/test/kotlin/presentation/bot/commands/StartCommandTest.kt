package presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ChatMember
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.BotAdminUtils
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.presentation.bot.commands.StartCommand
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class StartCommandTest {

    private val autoRegisterService: AutoRegisterService = mockk()
    private val botAdminUtils: BotAdminUtils = mockk()
    private val bot: Bot = mockk(relaxed = true)
    private lateinit var startCommand: StartCommand

    private val chatId = 123L
    private val userId = 456L
    private val user = User(id = userId, isBot = false, firstName = "Alice", username = "alice")
    private val member = Member(1L, chatId, userId, "alice", "Alice", null)

    @BeforeTest
    fun setup() {
        clearAllMocks()
        startCommand = StartCommand(autoRegisterService, botAdminUtils)
        coEvery {
            autoRegisterService.ensureUserRegistered(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns ResultContainer.success(member)
        every { botAdminUtils.getMemberRole(any(), any(), any()) } returns MemberRole.MEMBER
    }

    private fun createUpdate(
        fromUser: User? = user,
        chatIdVal: Long = chatId,
        chatType: String = "private"
    ): Update {
        val chat = Chat(id = chatIdVal, type = chatType)
        val message = Message(messageId = 1L, date = 0L, chat = chat, from = fromUser, text = "/start")
        return Update(updateId = 1L, message = message)
    }

    @Test
    fun `invoke should send welcome message`() = runTest {
        // Given
        val update = createUpdate()
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "private")
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("Spovishun активний") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `invoke should register trigger user`() = runTest {
        // Given
        val update = createUpdate()
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "private")
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify {
            autoRegisterService.ensureUserRegistered(
                userId = userId,
                chatId = chatId,
                username = "alice",
                firstName = "Alice",
                userRole = MemberRole.MEMBER
            )
        }
    }

    @Test
    fun `invoke should register admins for group chat`() = runTest {
        // Given
        val update = createUpdate(chatType = "group")
        val adminUser = User(id = 789L, isBot = false, firstName = "Admin", username = "admin")
        val adminChatMember = ChatMember(user = adminUser, status = "administrator")
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "group")
        )
        every { bot.getChatAdministrators(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            listOf(
                adminChatMember
            )
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify {
            autoRegisterService.ensureUserRegistered(
                userId = 789L,
                chatId = chatId,
                username = "admin",
                firstName = "Admin",
                userRole = MemberRole.MEMBER
            )
        }
    }

    @Test
    fun `invoke should register admins for supergroup chat`() = runTest {
        // Given
        val update = createUpdate(chatType = "supergroup")
        val adminUser = User(id = 789L, isBot = false, firstName = "Admin", username = "admin")
        val adminChatMember = ChatMember(user = adminUser, status = "administrator")
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "supergroup")
        )
        every { bot.getChatAdministrators(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            listOf(
                adminChatMember
            )
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify {
            autoRegisterService.ensureUserRegistered(
                userId = 789L,
                chatId = chatId,
                username = "admin",
                firstName = "Admin",
                userRole = MemberRole.MEMBER
            )
        }
    }

    @Test
    fun `invoke should send registration invitation for group chat`() = runTest {
        // Given
        val update = createUpdate(chatType = "group")
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "group")
        )
        every { bot.getChatAdministrators(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(emptyList())
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify(atLeast = 1) {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("Реєстрація учасників") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `invoke should sanitize username with special characters`() = runTest {
        // Given
        val specialUser = User(id = userId, isBot = false, firstName = "Alice", username = "al!ce@#")
        val update = createUpdate(fromUser = specialUser)
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "private")
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify {
            autoRegisterService.ensureUserRegistered(
                userId = userId,
                chatId = chatId,
                username = "al_ce__",
                firstName = "Alice",
                userRole = MemberRole.MEMBER
            )
        }
    }

    @Test
    fun `invoke should use user_id when username is null`() = runTest {
        // Given
        val noUsernameUser = User(id = userId, isBot = false, firstName = "Alice", username = null)
        val update = createUpdate(fromUser = noUsernameUser)
        every { bot.getChat(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(
            Chat(id = chatId, type = "private")
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        startCommand(bot, update)

        // Then
        coVerify {
            autoRegisterService.ensureUserRegistered(
                userId = userId,
                chatId = chatId,
                username = "user_$userId",
                firstName = "Alice",
                userRole = MemberRole.MEMBER
            )
        }
    }

    @Test
    fun `invoke should return early when message is null`() = runTest {
        // Given
        val update = Update(updateId = 1L, message = null)

        // When
        startCommand(bot, update)

        // Then
        coVerify(exactly = 0) { bot.sendMessage(any(), any(), any()) }
    }
}
