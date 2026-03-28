package presentation.bot.commands

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.Update
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.presentation.util.BotAdminUtils
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.GroupWithMembers
import com.ua.astrumon.domain.service.MemberService
import com.ua.astrumon.presentation.bot.commands.PingCommand
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class PingCommandTest {

    private val memberService: MemberService = mockk()
    private val groupService: GroupService = mockk()
    private val autoRegisterService: AutoRegisterService = mockk()
    private val botAdminUtils: BotAdminUtils = mockk()
    private val bot: Bot = mockk(relaxed = true)
    private lateinit var pingCommand: PingCommand

    private val chatId = 123L
    private val userId = 456L
    private val user = User(id = userId, isBot = false, firstName = "Alice", username = "alice")
    private val member = Member(1L, chatId, userId, "alice", "Alice", null)

    @BeforeTest
    fun setup() {
        clearAllMocks()
        pingCommand = PingCommand(memberService, groupService, autoRegisterService, botAdminUtils)
        coEvery { autoRegisterService.ensureUserRegistered(any(), any(), any(), any(), any()) } returns ResultContainer.success(member)
        every { botAdminUtils.getMemberRole(any(), any(), any()) } returns MemberRole.MEMBER
    }

    private fun createUpdate(
        fromUser: User? = user,
        chatIdVal: Long = chatId,
        text: String = "/all"
    ): Update {
        val chat = Chat(id = chatIdVal, type = "group")
        val message = Message(messageId = 1L, date = 0L, chat = chat, from = fromUser, text = text)
        return Update(updateId = 1L, message = message)
    }

    // --- pingAll ---

    @Test
    fun `pingAll should send mentions for all members`() = runTest {
        // Given
        val update = createUpdate()
        val members = listOf(
            Member(1L, chatId, 456L, "alice", "Alice", null),
            Member(2L, chatId, 789L, "bob", "Bob", null)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingAll(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("@alice") && it.contains("@bob") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingAll should include extra text from args`() = runTest {
        // Given
        val update = createUpdate(text = "/all standup time")
        val members = listOf(Member(1L, chatId, 456L, "alice", "Alice", null))
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingAll(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("standup time") && it.contains("@alice") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingAll should send empty message when no members`() = runTest {
        // Given
        val update = createUpdate()
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(emptyList())
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingAll(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("Немає зареєстрованих учасників") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingAll should send error message on failure`() = runTest {
        // Given
        val update = createUpdate()
        coEvery { memberService.getAllMembers() } returns ResultContainer.failure(DatabaseException("error"))
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingAll(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("Помилка завантаження учасників") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingAll should return early when message is null`() = runTest {
        // Given
        val update = Update(updateId = 1L, message = null)

        // When
        pingCommand.pingAll(bot, update)

        // Then
        coVerify(exactly = 0) { memberService.getAllMembers() }
    }

    // --- pingGroup ---

    @Test
    fun `pingGroup should send mentions for group members`() = runTest {
        // Given
        val update = createUpdate(text = "/ping devs")
        val group = GroupWithMembers(1L, chatId, "devs", "devs", listOf("alice", "bob"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(group)
        coEvery { memberService.getMemberByUsername("alice") } returns ResultContainer.success(member)
        coEvery { memberService.getMemberByUsername("bob") } returns ResultContainer.success(
            Member(2L, chatId, 789L, "bob", "Bob", null)
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("@alice") && it.contains("@bob") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should include extra text from args`() = runTest {
        // Given
        val update = createUpdate(text = "/ping devs review please")
        val group = GroupWithMembers(1L, chatId, "devs", "devs", listOf("alice"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(group)
        coEvery { memberService.getMemberByUsername("alice") } returns ResultContainer.success(member)
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("review please") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should send usage message when no args`() = runTest {
        // Given
        val update = createUpdate(text = "/ping")
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("/ping") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should send not found message when group does not exist`() = runTest {
        // Given
        val update = createUpdate(text = "/ping unknown")
        coEvery { groupService.getGroupByKey(chatId, "unknown") } returns ResultContainer.failure(
            ResourceNotFoundException("Group", "unknown")
        )
        coEvery { groupService.getAllGroupsWithMembers(chatId) } returns ResultContainer.success(
            listOf(GroupWithMembers(1L, chatId, "devs", "devs", emptyList()))
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("не знайдено") && it.contains("devs") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should skip members not found in database`() = runTest {
        // Given
        val update = createUpdate(text = "/ping devs")
        val group = GroupWithMembers(1L, chatId, "devs", "devs", listOf("alice", "ghost"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(group)
        coEvery { memberService.getMemberByUsername("alice") } returns ResultContainer.success(member)
        coEvery { memberService.getMemberByUsername("ghost") } returns ResultContainer.failure(
            ResourceNotFoundException("Member", "ghost")
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("@alice") && !it.contains("@ghost") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should send no one to ping when all members invalid`() = runTest {
        // Given
        val update = createUpdate(text = "/ping devs")
        val group = GroupWithMembers(1L, chatId, "devs", "devs", listOf("ghost"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(group)
        coEvery { memberService.getMemberByUsername("ghost") } returns ResultContainer.failure(
            ResourceNotFoundException("Member", "ghost")
        )
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("Немає кого пінгувати") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should send no one to ping when group has no members`() = runTest {
        // Given
        val update = createUpdate(text = "/ping devs")
        val group = GroupWithMembers(1L, chatId, "devs", "devs", emptyList())
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(group)
        every { bot.sendMessage(any(), any(), any()) } returns mockk<TelegramBotResult<Message>>()

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify {
            bot.sendMessage(
                ChatId.fromId(chatId),
                match { it.contains("Немає кого пінгувати") },
                ParseMode.HTML
            )
        }
    }

    @Test
    fun `pingGroup should return early when message is null`() = runTest {
        // Given
        val update = Update(updateId = 1L, message = null)

        // When
        pingCommand.pingGroup(bot, update)

        // Then
        coVerify(exactly = 0) { groupService.getGroupByKey(any(), any()) }
    }
}
