package presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
import com.ua.astrumon.domain.service.MemberService
import com.ua.astrumon.domain.BotAdminUtils
import com.ua.astrumon.presentation.bot.commands.RegisterCommand
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class RegisterCommandTest {

    private val memberService: MemberService = mockk()
    private val botAdminUtils: BotAdminUtils = mockk()
    private val bot: Bot = mockk(relaxed = true)
    private lateinit var registerCommand: RegisterCommand

    private val chatId = 123L
    private val userId = 456L

    @BeforeTest
    fun setup() {
        clearAllMocks()
        registerCommand = RegisterCommand(memberService, botAdminUtils)
        every { botAdminUtils.getMemberRole(any(), any(), any()) } returns MemberRole.MEMBER
    }

    private fun createUpdate(
        fromUser: User? = User(id = userId, isBot = false, firstName = "Alice", username = "alice"),
        chatIdVal: Long = chatId
    ): Update {
        val chat = Chat(id = chatIdVal, type = "group")
        val message = Message(messageId = 1L, date = 0L, chat = chat, from = fromUser)
        return Update(updateId = 1L, message = message)
    }

    @Test
    fun `invoke should send success message when registration succeeds`() = runTest {
        // Given
        val update = createUpdate()
        val member = Member(1L, chatId, userId, "alice", "Alice", null)
        coEvery { memberService.createMember(chatId, userId, "alice", "Alice", MemberRole.MEMBER) } returns ResultContainer.success(member)
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        registerCommand(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("успішно зареєстровані") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `invoke should send already registered message when registration fails`() = runTest {
        // Given
        val update = createUpdate()
        coEvery { memberService.createMember(chatId, userId, "alice", "Alice", MemberRole.MEMBER) } returns
                ResultContainer.failure(DuplicateResourceException("Member", "alice"))
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        registerCommand(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("вже зареєстровані") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `invoke should use user_id as username when username is null`() = runTest {
        // Given
        val user = User(id = userId, isBot = false, firstName = "Alice", username = null)
        val update = createUpdate(fromUser = user)
        val member = Member(1L, chatId, userId, "user_$userId", "Alice", null)
        coEvery { memberService.createMember(chatId, userId, "user_$userId", "Alice", MemberRole.MEMBER) } returns ResultContainer.success(member)
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        registerCommand(bot, update)

        // Then
        coVerify { memberService.createMember(chatId, userId, "user_$userId", "Alice", MemberRole.MEMBER) }
    }

    @Test
    fun `invoke should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        registerCommand(bot, update)

        // Then
        coVerify(exactly = 0) { memberService.createMember(any(), any(), any(), any()) }
        coVerify(exactly = 0) { bot.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `invoke should return early when message is null`() = runTest {
        // Given
        val update = Update(updateId = 1L, message = null)

        // When
        registerCommand(bot, update)

        // Then
        coVerify(exactly = 0) { memberService.createMember(any(), any(), any(), any()) }
    }
}
