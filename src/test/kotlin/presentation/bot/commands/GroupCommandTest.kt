package presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.ua.astrumon.presentation.bot.commands.GroupCommand
import com.ua.astrumon.presentation.controller.GroupController
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GroupCommandTest {

    private val groupController: GroupController = mockk()
    private val bot: Bot = mockk(relaxed = true)
    private lateinit var groupCommand: GroupCommand

    private val chatId = 123L
    private val userId = 456L
    private val user = User(id = userId, isBot = false, firstName = "Alice", username = "alice")

    @BeforeTest
    fun setup() {
        clearAllMocks()
        groupCommand = GroupCommand(groupController)
    }

    private fun createUpdate(
        fromUser: User? = user,
        chatIdVal: Long = chatId,
        text: String = "/groups"
    ): Update {
        val chat = Chat(id = chatIdVal, type = "group")
        val message = Message(messageId = 1L, date = 0L, chat = chat, from = fromUser, text = text)
        return Update(updateId = 1L, message = message)
    }

    // --- showGroups ---

    @Test
    fun `showGroups should call controller and send message`() = runTest {
        // Given
        val update = createUpdate()
        coEvery { groupController.getGroups(chatId, any()) } returns "groups list"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        groupCommand.showGroups(bot, update)

        // Then
        coVerify { groupController.getGroups(chatId, any()) }
        coVerify { bot.sendMessage(ChatId.fromId(chatId), "groups list", ParseMode.HTML) }
    }

    @Test
    fun `showGroups should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        groupCommand.showGroups(bot, update)

        // Then
        coVerify(exactly = 0) { groupController.getGroups(any(), any()) }
    }

    // --- addNewGroup ---

    @Test
    fun `addNewGroup should pass args to controller`() = runTest {
        // Given
        val update = createUpdate(text = "/newgroup devs")
        coEvery { groupController.createGroup(bot, chatId, userId, listOf("devs")) } returns "created"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        groupCommand.addNewGroup(bot, update)

        // Then
        coVerify { groupController.createGroup(bot, chatId, userId, listOf("devs")) }
        coVerify { bot.sendMessage(ChatId.fromId(chatId), "created", ParseMode.HTML) }
    }

    @Test
    fun `addNewGroup should pass empty args when no arguments`() = runTest {
        // Given
        val update = createUpdate(text = "/newgroup")
        coEvery { groupController.createGroup(bot, chatId, userId, emptyList()) } returns "usage"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        groupCommand.addNewGroup(bot, update)

        // Then
        coVerify { groupController.createGroup(bot, chatId, userId, emptyList()) }
    }

    @Test
    fun `addNewGroup should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        groupCommand.addNewGroup(bot, update)

        // Then
        coVerify(exactly = 0) { groupController.createGroup(any(), any(), any(), any()) }
    }

    // --- deleteGroup ---

    @Test
    fun `deleteGroup should pass args to controller`() = runTest {
        // Given
        val update = createUpdate(text = "/delgroup devs")
        coEvery { groupController.deleteGroup(bot, chatId, userId, listOf("devs")) } returns "deleted"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        groupCommand.deleteGroup(bot, update)

        // Then
        coVerify { groupController.deleteGroup(bot, chatId, userId, listOf("devs")) }
        coVerify { bot.sendMessage(ChatId.fromId(chatId), "deleted", ParseMode.HTML) }
    }

    @Test
    fun `deleteGroup should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        groupCommand.deleteGroup(bot, update)

        // Then
        coVerify(exactly = 0) { groupController.deleteGroup(any(), any(), any(), any()) }
    }

    // --- addUserToGroup ---

    @Test
    fun `addUserToGroup should pass args to controller`() = runTest {
        // Given
        val update = createUpdate(text = "/addtogroup devs @bob")
        coEvery { groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob")) } returns "added"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        groupCommand.addUserToGroup(bot, update)

        // Then
        coVerify { groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob")) }
    }

    @Test
    fun `addUserToGroup should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        groupCommand.addUserToGroup(bot, update)

        // Then
        coVerify(exactly = 0) { groupController.addUserToGroup(any(), any(), any(), any()) }
    }

    // --- removeUserFromGroup ---

    @Test
    fun `removeUserFromGroup should pass args to controller`() = runTest {
        // Given
        val update = createUpdate(text = "/removefromgroup devs @bob")
        coEvery { groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs", "@bob")) } returns "removed"
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        groupCommand.removeUserFromGroup(bot, update)

        // Then
        coVerify { groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs", "@bob")) }
    }

    @Test
    fun `removeUserFromGroup should return early when user is null`() = runTest {
        // Given
        val update = createUpdate(fromUser = null)

        // When
        groupCommand.removeUserFromGroup(bot, update)

        // Then
        coVerify(exactly = 0) { groupController.removeUserFromGroup(any(), any(), any(), any()) }
    }
}
