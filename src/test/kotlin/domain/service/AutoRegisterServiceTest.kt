package domain.service

import com.ua.astrumon.common.exception.DatabaseException
import com.ua.astrumon.common.exception.DuplicateResourceException
import com.ua.astrumon.common.exception.ResourceNotFoundException
import com.ua.astrumon.common.exception.ValidationException
import com.ua.astrumon.common.result.ResultContainer
import com.ua.astrumon.domain.model.Member
import com.ua.astrumon.domain.service.AutoRegisterService
import com.ua.astrumon.domain.service.MemberService
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoRegisterServiceTest {

    private val memberService: MemberService = mockk()
    private lateinit var autoRegisterService: AutoRegisterService

    @BeforeTest
    fun setup() {
        clearAllMocks()
        autoRegisterService = AutoRegisterService(memberService)
    }

    @Test
    fun `ensureUserRegistered should return existing member when user already exists`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val username = "alice"
        val firstName = "Alice"
        val existingMember = Member(1L, chatId, userId, username, firstName, null)

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.success(existingMember)

        // When
        val result = autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(existingMember, result.getOrThrow())
        coVerify { memberService.getMemberByUsername(username) }
        coVerify(exactly = 0) { memberService.createMember(any(), any(), any(), any()) }
    }

    @Test
    fun `ensureUserRegistered should create new member when user does not exist`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val username = "alice"
        val firstName = "Alice"
        val newMember = Member(1L, chatId, userId, username, firstName, null)
        val notFoundError = ResourceNotFoundException("Member", username)

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.failure(notFoundError)
        coEvery { memberService.createMember(chatId, userId, username, firstName) } returns ResultContainer.success(newMember)

        // When
        val result = autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(newMember, result.getOrThrow())
        coVerify { memberService.getMemberByUsername(username) }
        coVerify { memberService.createMember(chatId, userId, username, firstName) }
    }

    @Test
    fun `ensureUserRegistered should return validation error when userId is invalid`() = runTest {
        // Given
        val chatId = 123L
        val userId = -1L
        val username = "alice"
        val firstName = "Alice"

        // When
        val result = autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Cannot register user with invalid userId: -1", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { memberService.getMemberByUsername(any()) }
        coVerify(exactly = 0) { memberService.createMember(any(), any(), any(), any()) }
    }

    @Test
    fun `ensureUserRegistered should return failure when member creation fails`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val username = "alice"
        val firstName = "Alice"
        val notFoundError = ResourceNotFoundException("Member", username)

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.failure(notFoundError)
        coEvery { memberService.createMember(chatId, userId, username, firstName) } returns ResultContainer.failure(
            DuplicateResourceException("Member", username)
        )

        // When
        val result = autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName)

        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is DuplicateResourceException)
        assertEquals("Member with identifier '$username' already exists", exception.message)
        coVerify { memberService.getMemberByUsername(username) }
        coVerify { memberService.createMember(chatId, userId, username, firstName) }
    }

    @Test
    fun `ensureUserRegistered should return failure when member lookup fails`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val username = "alice"
        val firstName = "Alice"
        val error = DatabaseException("Database error")

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.failure(error)

        // When
        val result = autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName)

        // Then
        assertTrue(result.isFailure)
        coVerify { memberService.getMemberByUsername(username) }
        coVerify(exactly = 0) { memberService.createMember(any(), any(), any(), any()) }
    }

    @Test
    fun `ensureUserRegistered should handle unexpected exception and return DatabaseException`() = runTest {
        // Given
        val chatId = 123L
        val userId = 456L
        val username = "alice"
        val firstName = "Alice"
        val unexpectedError = RuntimeException("Unexpected error")

        coEvery { memberService.getMemberByUsername(username) } throws unexpectedError

        // When
        val result = autoRegisterService.ensureUserRegistered(chatId, userId, username, firstName)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DatabaseException)
        val dbException = result.exceptionOrNull() as DatabaseException
        assertEquals("Database operation failed: Unexpected error during auto-registration", dbException.message)
        assertEquals(unexpectedError, dbException.cause)
        coVerify { memberService.getMemberByUsername(username) }
    }

    @Test
    fun `isUserRegistered should return true when member exists`() = runTest {
        // Given
        val username = "alice"
        val member = Member(1L, 123L, 456L, username, "Alice", null)

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.success(member)

        // When
        val result = autoRegisterService.isUserRegistered(username)

        // Then
        assertTrue(result)
        coVerify { memberService.getMemberByUsername(username) }
    }

    @Test
    fun `isUserRegistered should return false when member does not exist`() = runTest {
        // Given
        val username = "alice"

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.failure(
            ResourceNotFoundException("Member", username)
        )

        // When
        val result = autoRegisterService.isUserRegistered(username)

        // Then
        assertFalse(result)
        coVerify { memberService.getMemberByUsername(username) }
    }

    @Test
    fun `isUserRegistered should return false when member lookup fails`() = runTest {
        // Given
        val username = "alice"

        coEvery { memberService.getMemberByUsername(username) } returns ResultContainer.failure(
            DatabaseException("Database error")
        )

        // When
        val result = autoRegisterService.isUserRegistered(username)

        // Then
        assertFalse(result)
        coVerify { memberService.getMemberByUsername(username) }
    }

}
