package presentation.controller

import com.github.kotlintelegrambot.Bot
import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.BotAdminUtils
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
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
import kotlin.test.assertTrue

class MembersControllerTest {

    private val memberService: MemberService = mockk()
    private val autoRegisterService: AutoRegisterService = mockk()
    private val botAdminUtils: BotAdminUtils = mockk()
    private lateinit var membersController: MembersController
    private val bot: Bot = mockk()

    private val chatId = 123L
    private val userId = 456L
    private val username = "alice"
    private val firstName = "Alice"
    private val userRole = MemberRole.MEMBER
    private val member = Member(1L, chatId, userId, username, firstName, null)

    @BeforeTest
    fun setup() {
        clearAllMocks()
        membersController = MembersController(memberService, autoRegisterService, botAdminUtils)
        coEvery { autoRegisterService.ensureUserRegistered(any(), any(), any(), any(), any()) } returns ResultContainer.success(member)
        every { botAdminUtils.getMemberRole(any(), any(), any()) } returns MemberRole.MEMBER
    }

    @Test
    fun `getMembers should return formatted list with role badges`() = runTest {
        // Given
        val members = listOf(
            Member(1L, chatId, 456L, "alice", "Alice", null, role = MemberRole.ADMIN),
            Member(2L, chatId, 789L, "bob", "Bob", null, role = MemberRole.MODERATOR),
            Member(3L, chatId, 111L, "charlie", "Charlie", null, role = MemberRole.MEMBER)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("Зареєстровані учасники:"))
        assertTrue(result.contains("@alice \uD83D\uDD10"))
        assertTrue(result.contains("@bob \uD83D\uDEE1"))
        assertTrue(result.contains("@charlie"))
        assertTrue(!result.contains("charlie \uD83D\uDD10"))
        assertTrue(result.contains("Всього: 3 учасників"))
        coVerify { autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName, userRole) }
    }

    @Test
    fun `getMembers should show firstName for user_ prefixed usernames`() = runTest {
        // Given
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
    fun `getMembers should return empty message when no members`() = runTest {
        // Given
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(emptyList())

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("Немає зареєстрованих учасників"))
    }

    @Test
    fun `getMembers should return error message on failure`() = runTest {
        // Given
        val error = DatabaseException("Connection lost")
        coEvery { memberService.getAllMembers() } returns ResultContainer.failure(error)

        // When
        val result = membersController.getMembers(bot, chatId, member)

        // Then
        assertTrue(result.contains("Помилка завантаження учасників"))
        assertTrue(result.contains(error.userMessage))
    }

    @Test
    fun `getMembers should handle admin auto-registration successfully`() = runTest {
        // Given
        val adminMember = Member(1L, chatId, userId, "admin_alice", "Admin Alice", null, role = MemberRole.ADMIN)
        val members = listOf(
            adminMember,
            Member(2L, chatId, 789L, "bob", "Bob", null, role = MemberRole.MEMBER)
        )
        coEvery { memberService.getAllMembers() } returns ResultContainer.success(members)
        every { botAdminUtils.getMemberRole(any(), any(), any()) } returns MemberRole.ADMIN
        coEvery { autoRegisterService.ensureUserRegistered(any(), any(), any(), any(), eq(MemberRole.ADMIN)) } returns ResultContainer.success(adminMember)

        // When
        val result = membersController.getMembers(bot, chatId, adminMember)

        // Then
        assertTrue(result.contains("Зареєстровані учасники:"))
        assertTrue(result.contains("@admin_alice \uD83D\uDD10"))
        assertTrue(result.contains("@bob"))
        assertTrue(result.contains("Всього: 2 учасників"))
        coVerify { autoRegisterService.ensureUserRegistered(chatId, userId, "admin_alice", "Admin Alice", MemberRole.ADMIN) }
    }
}
