package presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.ua.astrumon.presentation.bot.commands.MembersCommand
import com.ua.astrumon.presentation.controller.MembersController
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MembersCommandTest {

    private val membersController: MembersController = mockk()
    private val bot: Bot = mockk(relaxed = true)
    private lateinit var membersCommand: MembersCommand

    private val chatId = 123L
    private val userId = 456L

    @BeforeTest
    fun setup() {
        clearAllMocks()
        membersCommand = MembersCommand(membersController)
    }

    private fun createUpdate(
        fromUser: User? = User(id = userId, isBot = false, firstName = "Alice", username = "alice"),
        chatIdVal: Long = chatId,
        text: String = "/members"
    ): Update {
        val chat = Chat(id = chatIdVal, type = "group")
        val message = Message(
            messageId = 1L,
            date = 0L,
            chat = chat,
            from = fromUser,
            text = text
        )
        return Update(updateId = 1L, message = message)
    }

    @Test
    fun `invoke should call controller and send message`() = runTest {
        // Given
        val update = createUpdate()
        coEvery { membersController.getMembers(bot, chatId, any()) } returns "members list"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        membersCommand(bot, update)

        // Then
        coVerify { membersController.getMembers(bot, chatId, any()) }
        coVerify { bot.sendMessage(ChatId.fromId(chatId), "members list", ParseMode.HTML) }
    }

    @Test
    fun `invoke should use user_id as username when username is null`() = runTest {
        // Given
        val user = User(id = userId, isBot = false, firstName = "Alice", username = null)
        val update = createUpdate(fromUser = user)
        coEvery { membersController.getMembers(bot, chatId, match { it.username == "user_$userId" }) } returns "ok"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        membersCommand(bot, update)

        // Then
        coVerify { membersController.getMembers(bot,chatId, match { it.username == "user_$userId" }) }
    }

    @Test
    fun `invoke should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        membersCommand(bot, update)

        // Then
        coVerify(exactly = 0) { membersController.getMembers(any(), any(), any()) }
        coVerify(exactly = 0) { bot.sendMessage(any(), any(), any()) }
    }

    @Test
    fun `invoke should return early when message is null`() = runTest {
        // Given
        val update = Update(updateId = 1L, message = null)

        // When
        membersCommand(bot, update)

        // Then
        coVerify(exactly = 0) { membersController.getMembers(any(), any(), any()) }
    }
}
