package com.lvlup.backend.unit

import com.lvlup.backend.TestFixtures
import com.lvlup.backend.dto.ChangePasswordRequest
import com.lvlup.backend.dto.LoginRequest
import com.lvlup.backend.exception.InvalidCredentialsException
import com.lvlup.backend.exception.UserAlreadyExistsException
import com.lvlup.backend.model.UserRole
import com.lvlup.backend.repository.UserRepository
import com.lvlup.backend.security.jwt.JwtTokenProvider
import com.lvlup.backend.security.principle.UserDetailsImpl
import com.lvlup.backend.service.AuthenticationService
import com.lvlup.backend.service.UserProfileService
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {

    private lateinit var authService: AuthenticationService
    private lateinit var profileService: UserProfileService
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authenticationManager: AuthenticationManager

    @BeforeEach
    fun setup() {
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtTokenProvider = mockk()
        authenticationManager = mockk()

        authService = AuthenticationService(
            userRepository,
            passwordEncoder,
            jwtTokenProvider,
            authenticationManager
        )
        profileService = UserProfileService(
            userRepository,
            passwordEncoder,
            authService,
        )

    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `register should create new user successfully`() {
        // Given
        val request = TestFixtures.createRegisterRequest()
        val encodedPassword = "encoded_password"
        val savedUser = TestFixtures.createUserFixture(
            email = request.email,
            password = encodedPassword,
        )
        val token = "jwt_token"
        val refreshToken = "refresh_token"

        every { userRepository.existsUserByEmail(request.email) } returns false
        every { passwordEncoder.encode(request.password) } returns encodedPassword
        every { userRepository.createUser(any()) } returns savedUser
        every { jwtTokenProvider.generateToken(any()) } returns token
        every { jwtTokenProvider.generateToken(any(), true) } returns refreshToken

        // When
        val response = authService.registerNewUser(request)

        // Then
        assertNotNull(response)
        Assertions.assertEquals(token, response.token)
        Assertions.assertEquals(refreshToken, response.refreshToken)
        Assertions.assertEquals("Bearer", response.type)
        Assertions.assertEquals(savedUser.email, response.user.email)
        Assertions.assertEquals(UserRole.ROLE_USER, response.user.role)

        verify(exactly = 1) { userRepository.existsUserByEmail(request.email) }
        verify(exactly = 1) { passwordEncoder.encode(request.password) }
        verify(exactly = 1) { userRepository.createUser(any()) }
        verify(exactly = 1) { jwtTokenProvider.generateToken(any()) }
        verify(exactly = 1) { jwtTokenProvider.generateToken(any(), true) }
    }

    @Test
    fun `register should throw exception when email already exists`() {
        // Given
        val request = TestFixtures.createRegisterRequest()
        every { userRepository.existsUserByEmail(request.email) } returns true

        // When/Then
        val exception = assertThrows<UserAlreadyExistsException> {
            authService.registerNewUser(request)
        }

        Assertions.assertTrue(exception.message!!.contains(request.email))

        verify(exactly = 1) { userRepository.existsUserByEmail(request.email) }
        verify(exactly = 0) { userRepository.createUser(any()) }
    }

    @Test
    fun `login should authenticate user successfully`() {
        // Given
        val request = LoginRequest(email = "test@example.com", password = "TestPass123!")
        val user = TestFixtures.createUserFixture(email = request.email)
        val userPrincipal = UserDetailsImpl(user)
        val authentication = UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.authorities)
        val token = "jwt_token"
        val refreshToken = "refresh_token"

        every { authenticationManager.authenticate(any()) } returns authentication
        every { jwtTokenProvider.generateToken(any()) } returns token
        every { jwtTokenProvider.generateToken(any(), true) } returns refreshToken

        // When
        val response = authService.login(request)

        // Then
        assertNotNull(response)
        Assertions.assertEquals(token, response.token)
        Assertions.assertEquals(refreshToken, response.refreshToken)
        Assertions.assertEquals(user.email, response.user.email)

        verify(exactly = 1) { authenticationManager.authenticate(any()) }
    }

    @Test
    fun `login should throw exception for invalid credentials`() {
        // Given
        val request = LoginRequest(email = "test@example.com", password = "WrongPassword!")
        every { authenticationManager.authenticate(any()) } throws BadCredentialsException("Bad credentials")

        // When/Then
        assertThrows<InvalidCredentialsException> {
            authService.login(request)
        }

        verify(exactly = 1) { authenticationManager.authenticate(any()) }
    }

    @Test
    fun `changePassword should update password successfully`() {
        // Given
        val userEmail = "test@example.com"
        val request = ChangePasswordRequest(
            currentPassword = "OldPass123!",
            newPassword = "NewPass123!"
        )
        val user = TestFixtures.createUserFixture(email = userEmail)
        val newPasswordHash = "new_encoded_password"

        every { userRepository.findUserByEmail(userEmail) } returns user
        every { passwordEncoder.matches(request.currentPassword, user.passwordHash) } returns true
        every { passwordEncoder.encode(request.newPassword) } returns newPasswordHash
        every { userRepository.updatePassword(userEmail, newPasswordHash) } returns 1

        // When
        profileService.changePassword(userEmail, request)

        // Then
        verify(exactly = 1) { userRepository.findUserByEmail(userEmail) }
        verify(exactly = 1) { passwordEncoder.matches(request.currentPassword, user.passwordHash) }
        verify(exactly = 1) { passwordEncoder.encode(request.newPassword) }
        verify(exactly = 1) { userRepository.updatePassword(userEmail, newPasswordHash) }
    }

    @Test
    fun `changePassword should throw exception when current password is incorrect`() {
        // Given
        val userEmail = "test@example.com"
        val request = ChangePasswordRequest(
            currentPassword = "WrongPassword!",
            newPassword = "NewPass123!"
        )
        val user = TestFixtures.createUserFixture(email = userEmail)

        every { userRepository.findUserByEmail(userEmail) } returns user
        every { passwordEncoder.matches(request.currentPassword, user.passwordHash) } returns false

        // When/Then
        val exception = assertThrows<InvalidCredentialsException> {
            profileService.changePassword(userEmail, request)
        }

        Assertions.assertTrue(exception.message!!.contains("incorrect"))

        verify(exactly = 1) { userRepository.findUserByEmail(userEmail) }
        verify(exactly = 1) { passwordEncoder.matches(request.currentPassword, user.passwordHash) }
        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }

    @Test
    fun `changePassword should throw exception when new password same as old`() {
        // Given
        val userEmail = "test@example.com"
        val samePassword = "SamePass123!"
        val request = ChangePasswordRequest(
            currentPassword = samePassword,
            newPassword = samePassword
        )
        val user = TestFixtures.createUserFixture(email = userEmail)

        every { userRepository.findUserByEmail(userEmail) } returns user
        every { passwordEncoder.matches(request.currentPassword, user.passwordHash) } returns true

        // When/Then
        val exception = assertThrows<InvalidCredentialsException> {
            profileService.changePassword(userEmail, request)
        }

        Assertions.assertTrue(exception.message!!.contains("must be different"))

        verify(exactly = 0) { userRepository.updatePassword(any(), any()) }
    }
}