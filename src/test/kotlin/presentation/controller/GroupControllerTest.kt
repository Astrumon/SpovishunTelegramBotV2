package presentation.controller

import com.github.kotlintelegrambot.Bot
import com.ua.astrumon.common.exception.BusinessException
import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.exception.ValidationException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Group
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.model.MemberRole
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.GroupService
import com.ua.astrumon.domain.service.GroupWithMembers
import com.ua.astrumon.domain.service.MemberService
import com.ua.astrumon.presentation.controller.GroupController
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupControllerTest {

    private val groupService: GroupService = mockk()
    private val memberService: MemberService = mockk()
    private val autoRegisterService: AutoRegisterService = mockk()
    private val bot: Bot = mockk()
    private lateinit var groupController: GroupController

    private val chatId = 123L
    private val userId = 456L
    private val username = "alice"
    private val firstName = "Alice"
    private val adminMember = Member(1L, chatId, userId, username, firstName, null, role = MemberRole.ADMIN)
    private val member = Member(1L, chatId, userId, username, firstName, null, role = MemberRole.MEMBER)

    @BeforeTest
    fun setup() {
        clearAllMocks()
        groupController = GroupController(groupService, memberService, autoRegisterService)
        coEvery { autoRegisterService.ensureUserRegistered(any(), any(), any(), any()) } returns ResultContainer.success(adminMember)
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(adminMember)
    }

    // --- getGroups tests ---

    @Test
    fun `getGroups should return formatted list with badges`() = runTest {
        // Given
        val moderatorMember = Member(2L, chatId, 789L, "bob", "Bob", null, role = MemberRole.MODERATOR)
        val regularMember = Member(3L, chatId, 111L, "charlie", "Charlie", null, role = MemberRole.MEMBER)
        val groups = listOf(
            GroupWithMembers(1L, chatId, "devs", "devs", listOf("alice", "bob", "charlie")),
            GroupWithMembers(2L, chatId, "qa", "qa", emptyList())
        )
        coEvery { groupService.getAllGroupsWithMembers(chatId) } returns ResultContainer.success(groups)
        coEvery { memberService.getMemberByUsername("alice") } returns ResultContainer.success(adminMember)
        coEvery { memberService.getMemberByUsername("bob") } returns ResultContainer.success(moderatorMember)
        coEvery { memberService.getMemberByUsername("charlie") } returns ResultContainer.success(regularMember)

        // When
        val result = groupController.getGroups(chatId, adminMember)

        // Then
        assertTrue(result.contains("Групи:"))
        assertTrue(result.contains("@alice \uD83D\uDD10"))
        assertTrue(result.contains("@bob \uD83D\uDEE1"))
        assertTrue(result.contains("@charlie"))
        assertTrue(!result.contains("charlie \uD83D\uDD10"))
        assertTrue(result.contains("—"))
    }

    @Test
    fun `getGroups should handle member lookup failure gracefully`() = runTest {
        // Given
        val groups = listOf(GroupWithMembers(1L, chatId, "devs", "devs", listOf("unknown")))
        coEvery { groupService.getAllGroupsWithMembers(chatId) } returns ResultContainer.success(groups)
        coEvery { memberService.getMemberByUsername("unknown") } returns ResultContainer.failure(
            ResourceNotFoundException("Member", "unknown")
        )

        // When
        val result = groupController.getGroups(chatId, adminMember)

        // Then
        assertTrue(result.contains("@unknown"))
        assertTrue(!result.contains("\uD83D\uDD10"))
    }

    @Test
    fun `getGroups should return empty message when no groups`() = runTest {
        // Given
        coEvery { groupService.getAllGroupsWithMembers(chatId) } returns ResultContainer.success(emptyList())

        // When
        val result = groupController.getGroups(chatId, adminMember)

        // Then
        assertTrue(result.contains("Немає груп"))
    }

    @Test
    fun `getGroups should return error message on failure`() = runTest {
        // Given
        val error = DatabaseException("Connection lost")
        coEvery { groupService.getAllGroupsWithMembers(chatId) } returns ResultContainer.failure(error)

        // When
        val result = groupController.getGroups(chatId, adminMember)

        // Then
        assertTrue(result.contains("Помилка завантаження груп"))
    }

    // --- createGroup tests ---

    @Test
    fun `createGroup should return success message when created`() = runTest {
        // Given
        val group = Group(1L, chatId, "devs", emptyList())
        coEvery { groupService.createGroup(chatId, "devs") } returns ResultContainer.success(group)

        // When
        val result = groupController.createGroup(bot, chatId, userId, listOf("Devs"))

        // Then
        assertTrue(result.contains("Група"))
        assertTrue(result.contains("devs"))
        assertTrue(result.contains("створена"))
    }

    @Test
    fun `createGroup should return error when caller is regular member`() = runTest {
        // Given
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(member)

        // When
        val result = groupController.createGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("🚫"))
        coVerify(exactly = 0) { groupService.createGroup(any(), any()) }
    }

    @Test
    fun `createGroup should allow moderator`() = runTest {
        // Given
        val moderatorMember = Member(1L, chatId, userId, username, firstName, null, role = MemberRole.MODERATOR)
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(moderatorMember)
        val group = Group(1L, chatId, "devs", emptyList())
        coEvery { groupService.createGroup(chatId, "devs") } returns ResultContainer.success(group)

        // When
        val result = groupController.createGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("створена"))
    }

    @Test
    fun `createGroup should return usage message when no args`() = runTest {
        // When
        val result = groupController.createGroup(bot, chatId, userId, emptyList())

        // Then
        assertTrue(result.contains("/newgroup"))
    }

    @Test
    fun `createGroup should return duplicate error when group exists`() = runTest {
        // Given
        coEvery { groupService.createGroup(chatId, "devs") } returns ResultContainer.failure(
            DuplicateResourceException("Group", "devs")
        )

        // When
        val result = groupController.createGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("вже існує"))
    }

    // --- deleteGroup tests ---

    @Test
    fun `deleteGroup should return success message when deleted`() = runTest {
        // Given
        val groupWithMembers = GroupWithMembers(1L, chatId, "devs", "devs", emptyList())
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(groupWithMembers)
        coEvery { groupService.deleteGroup(chatId, "devs") } returns ResultContainer.success(Unit)

        // When
        val result = groupController.deleteGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("devs"))
        assertTrue(result.contains("видалена"))
    }

    @Test
    fun `deleteGroup should return error when caller is regular member`() = runTest {
        // Given
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(member)

        // When
        val result = groupController.deleteGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("🚫"))
    }

    @Test
    fun `deleteGroup should return usage message when no args`() = runTest {
        // When
        val result = groupController.deleteGroup(bot, chatId, userId, emptyList())

        // Then
        assertTrue(result.contains("/delgroup"))
    }

    @Test
    fun `deleteGroup should return not found error when group does not exist`() = runTest {
        // Given
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.failure(
            ResourceNotFoundException("Group", "devs")
        )

        // When
        val result = groupController.deleteGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("не знайдено"))
    }

    // --- addUserToGroup tests ---

    @Test
    fun `addUserToGroup should return success message when user added`() = runTest {
        // Given
        val groupWithMembers = GroupWithMembers(1L, chatId, "devs", "devs", listOf("alice"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(groupWithMembers)
        coEvery { groupService.addMemberToGroup(chatId, "devs", "bob") } returns ResultContainer.success(Unit)

        // When
        val result = groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("bob"))
        assertTrue(result.contains("додано"))
        assertTrue(result.contains("devs"))
    }

    @Test
    fun `addUserToGroup should return error when caller is regular member`() = runTest {
        // Given
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(member)

        // When
        val result = groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("🚫"))
    }

    @Test
    fun `addUserToGroup should return usage message when insufficient args`() = runTest {
        // When
        val result = groupController.addUserToGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("/addtogroup"))
    }

    @Test
    fun `addUserToGroup should return validation error when user invalid`() = runTest {
        // Given
        val groupWithMembers = GroupWithMembers(1L, chatId, "devs", "devs", emptyList())
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(groupWithMembers)
        coEvery { groupService.addMemberToGroup(chatId, "devs", "bob") } returns ResultContainer.failure(
            ValidationException("Invalid user")
        )

        // When
        val result = groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("Неможливо додати"))
    }

    @Test
    fun `addUserToGroup should return not found error when group does not exist`() = runTest {
        // Given
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.failure(
            ResourceNotFoundException("Group", "devs")
        )

        // When
        val result = groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("Групу"))
        assertTrue(result.contains("не знайдено"))
    }

    @Test
    fun `addUserToGroup should return duplicate error when user already in group`() = runTest {
        // Given
        val groupWithMembers = GroupWithMembers(1L, chatId, "devs", "devs", listOf("bob"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(groupWithMembers)
        coEvery { groupService.addMemberToGroup(chatId, "devs", "bob") } returns ResultContainer.failure(
            DuplicateResourceException("Member", "bob")
        )

        // When
        val result = groupController.addUserToGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("вже в групі"))
    }

    // --- removeUserFromGroup tests ---

    @Test
    fun `removeUserFromGroup should return success message when user removed`() = runTest {
        // Given
        val groupWithMembers = GroupWithMembers(1L, chatId, "devs", "devs", listOf("bob"))
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(groupWithMembers)
        coEvery { groupService.removeMemberFromGroup(chatId, "devs", "bob") } returns ResultContainer.success(Unit)

        // When
        val result = groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("bob"))
        assertTrue(result.contains("видалено"))
    }

    @Test
    fun `removeUserFromGroup should return error when caller is regular member`() = runTest {
        // Given
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(member)

        // When
        val result = groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("🚫"))
    }

    @Test
    fun `removeUserFromGroup should return usage message when insufficient args`() = runTest {
        // When
        val result = groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs"))

        // Then
        assertTrue(result.contains("/removefromgroup"))
    }

    @Test
    fun `removeUserFromGroup should return not found error when group does not exist`() = runTest {
        // Given
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.failure(
            ResourceNotFoundException("Group", "devs")
        )

        // When
        val result = groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("не знайдено"))
    }

    @Test
    fun `removeUserFromGroup should return business error when user not in group`() = runTest {
        // Given
        val groupWithMembers = GroupWithMembers(1L, chatId, "devs", "devs", emptyList())
        coEvery { groupService.getGroupByKey(chatId, "devs") } returns ResultContainer.success(groupWithMembers)
        coEvery { groupService.removeMemberFromGroup(chatId, "devs", "bob") } returns ResultContainer.failure(
            BusinessException("Member not in group")
        )

        // When
        val result = groupController.removeUserFromGroup(bot, chatId, userId, listOf("devs", "@bob"))

        // Then
        assertTrue(result.contains("не знайдено в групі"))
    }

    // --- grantRole tests ---

    @Test
    fun `grantRole should return success when admin grants moderator role`() = runTest {
        // Given
        val targetMember = Member(2L, chatId, 789L, "bob", "Bob", null, role = MemberRole.MEMBER)
        coEvery { memberService.getMemberByUsername("bob") } returns ResultContainer.success(targetMember)
        coEvery { memberService.setMemberRole(chatId, 789L, MemberRole.MODERATOR) } returns ResultContainer.success(
            targetMember.copy(role = MemberRole.MODERATOR)
        )

        // When
        val result = groupController.grantRole(chatId, userId, listOf("@bob", "moderator"))

        // Then
        assertTrue(result.contains("bob"))
        assertTrue(result.contains("moderator"))
    }

    @Test
    fun `grantRole should return error when caller is not admin`() = runTest {
        // Given
        coEvery { memberService.getMemberByChatAndUserId(chatId, userId) } returns ResultContainer.success(
            Member(1L, chatId, userId, username, firstName, null, role = MemberRole.MODERATOR)
        )

        // When
        val result = groupController.grantRole(chatId, userId, listOf("@bob", "moderator"))

        // Then
        assertTrue(result.contains("🚫"))
        coVerify(exactly = 0) { memberService.setMemberRole(any(), any(), any()) }
    }

    @Test
    fun `grantRole should return error when target user not found`() = runTest {
        // Given
        coEvery { memberService.getMemberByUsername("bob") } returns ResultContainer.failure(
            ResourceNotFoundException("Member", "bob")
        )

        // When
        val result = groupController.grantRole(chatId, userId, listOf("@bob", "moderator"))

        // Then
        assertTrue(result.contains("не знайдено"))
        coVerify(exactly = 0) { memberService.setMemberRole(any(), any(), any()) }
    }

    @Test
    fun `grantRole should return error for invalid role name`() = runTest {
        // When
        val result = groupController.grantRole(chatId, userId, listOf("@bob", "superadmin"))

        // Then
        assertTrue(result.contains("Невідома роль"))
        coVerify(exactly = 0) { memberService.setMemberRole(any(), any(), any()) }
    }

    @Test
    fun `grantRole should return usage message when insufficient args`() = runTest {
        // When
        val result = groupController.grantRole(chatId, userId, listOf("@bob"))

        // Then
        assertTrue(result.contains("/grantrole"))
    }
}
