package presentation.controller

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ChatMember
import com.github.kotlintelegrambot.entities.User
import com.github.kotlintelegrambot.types.TelegramBotResult
import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.MemberService
import com.ua.astrumon.presentation.controller.MembersController
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MembersControllerTest {

    private val memberService: MemberService = mockk()
    private val autoRegisterService: AutoRegisterService = mockk()
    private lateinit var membersController: MembersController

    private val chatId = 123L
    private val userId = 456L
    private val username = "alice"
    private val firstName = "Alice"
    private val member = Member(1L, chatId, userId, username, firstName, null)

    @BeforeTest
    fun setup() {
        clearAllMocks()
        membersController = MembersController(memberService, autoRegisterService)
        coEvery { autoRegisterService.ensureUserRegistered(any(), any(), any(), any()) } returns ResultContainer.success(member)
    }

    // --- getMembers(member) tests ---

    @Test
    fun `getMembers should return formatted list when members exist`() = runTest {
        // Given
        val members = listOf(
            Member(1L, chatId, 456L, "alice", "Alice", null),
            Member(2L, chatId, 789L, "bob", "Bob", null)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)

        // When
        val result = membersController.getMembers(member)

        // Then
        assertTrue(result.contains("Зареєстровані учасники:"))
        assertTrue(result.contains("@alice"))
        assertTrue(result.contains("@bob"))
        assertTrue(result.contains("Всього: 2 учасників"))
        coVerify { autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName) }
    }

    @Test
    fun `getMembers should show firstName for user_ prefixed usernames`() = runTest {
        // Given
        val members = listOf(
            Member(1L, chatId, 456L, "user_123", "NoUsername", null)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)

        // When
        val result = membersController.getMembers(member)

        // Then
        assertTrue(result.contains("• NoUsername"))
        assertTrue(!result.contains("@user_123"))
    }

    @Test
    fun `getMembers should return empty message when no members`() = runTest {
        // Given
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(emptyList())

        // When
        val result = membersController.getMembers(member)

        // Then
        assertTrue(result.contains("Немає зареєстрованих учасників"))
    }

    @Test
    fun `getMembers should return error message on failure`() = runTest {
        // Given
        val error = DatabaseException("Connection lost")
        coEvery { memberService.getAllMembers() } returns ResultContainer.failure(error)

        // When
        val result = membersController.getMembers(member)

        // Then
        assertTrue(result.contains("Помилка завантаження учасників"))
        assertTrue(result.contains(error.userMessage))
    }

    // --- getMembers(bot, chatId, member) tests ---

    @Test
    fun `getMembers with bot should show admin badge for admins`() = runTest {
        // Given
        val bot: Bot = mockk()
        val adminUser = User(id = 456L, isBot = false, firstName = "Alice")
        val adminChatMember = ChatMember(user = adminUser, status = "administrator")
        every { bot.getChatAdministrators(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(listOf(adminChatMember))

        val members = listOf(
            Member(1L, chatId, 456L, "alice", "Alice", null)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("@alice \uD83D\uDD10"))
    }

    @Test
    fun `getMembers with bot should not show admin badge for non-admins`() = runTest {
        // Given
        val bot: Bot = mockk()
        every { bot.getChatAdministrators(ChatId.fromId(chatId)) } returns TelegramBotResult.Success(emptyList())

        val members = listOf(
            Member(1L, chatId, 456L, "alice", "Alice", null)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("@alice"))
        assertTrue(!result.contains("\uD83D\uDD10"))
    }

    @Test
    fun `getMembers with bot should show firstName for user_ prefixed usernames`() = runTest {
        // Given
        val bot: Bot = mockk()
        val members = listOf(
            Member(1L, chatId, 456L, "user_123", "NoUsername", null)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("• NoUsername"))
        assertTrue(!result.contains("@user_123"))
    }

    @Test
    fun `getMembers with bot should return empty message when no members`() = runTest {
        // Given
        val bot: Bot = mockk()
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(emptyList())

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("Немає зареєстрованих учасників"))
    }

    @Test
    fun `getMembers with bot should return error message on failure`() = runTest {
        // Given
        val bot: Bot = mockk()
        val error = DatabaseException("Connection lost")
        coEvery { memberService.getAllMembers() } returns ResultContainer.failure(error)

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("Помилка завантаження учасників"))
    }
}
