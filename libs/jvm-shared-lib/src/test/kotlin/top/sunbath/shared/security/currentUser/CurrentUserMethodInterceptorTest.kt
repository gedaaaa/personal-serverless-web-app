package top.sunbath.shared.security.currentUser

import io.micronaut.aop.MethodInvocationContext
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.core.type.MutableArgumentValue
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.utils.SecurityService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.shared.types.UserInfo
import java.util.Optional

/**
 * Unit tests for the CurrentUserMethodInterceptor.
 */
@ExtendWith(MockKExtension::class)
class CurrentUserMethodInterceptorTest {
    @MockK
    private lateinit var securityService: SecurityService

    @MockK
    private lateinit var context: MethodInvocationContext<Any, Any>

    @MockK
    private lateinit var authentication: Authentication

    private lateinit var interceptor: CurrentUserMethodInterceptor

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        interceptor = CurrentUserMethodInterceptor(securityService)
    }

    @Test
    fun `test intercept with no CurrentUser parameters`() {
        // Given
        val expectedResult = "result"
        val parameters = emptyMap<String, MutableArgumentValue<*>>()

        every { context.parameters } returns parameters
        every { context.proceed() } returns expectedResult

        // When
        val result = interceptor.intercept(context)

        // Then
        assertEquals(expectedResult, result)

        // Verify
        verify(exactly = 1) { context.parameters }
        verify(exactly = 1) { context.proceed() }
        verify(exactly = 0) { securityService.authentication }
    }

    @Test
    fun `test intercept with CurrentUser parameter and authenticated user`() {
        // Given
        val paramName = "userInfo"
        val userId = "test-user-id"
        val username = "testuser"
        val email = "test@example.com"
        val attributes =
            mapOf(
                "sub" to userId,
                "username" to username,
                "email" to email,
            )
        val expectedResult = "result"

        // Mock parameter with @CurrentUser annotation
        val annotationMetadata = mockk<AnnotationMetadata>()
        every { annotationMetadata.hasAnnotation(CurrentUser::class.java) } returns true

        // Create a mutable argument value that we can capture what's set to it
        val userInfoParam = mockk<MutableArgumentValue<UserInfo>>()
        val capturedUserInfo = slot<UserInfo>()
        every { userInfoParam.type } returns UserInfo::class.java
        every { userInfoParam.setValue(capture(capturedUserInfo)) } returns Unit

        // Set up parameters map
        val parameters = mapOf(paramName to userInfoParam)

        every { context.parameters } returns parameters
        every { userInfoParam.annotationMetadata } returns annotationMetadata
        every { securityService.authentication } returns Optional.of(authentication)
        every { authentication.attributes } returns attributes
        every { context.proceed() } returns expectedResult

        // When
        val result = interceptor.intercept(context)

        // Then
        assertEquals(expectedResult, result)
        assertEquals(userId, capturedUserInfo.captured.id)
        assertEquals(username, capturedUserInfo.captured.username)
        assertEquals(email, capturedUserInfo.captured.email)

        // Verify
        verify(exactly = 1) { context.parameters }
        verify(exactly = 1) { securityService.authentication }
        verify(exactly = 1) { userInfoParam.setValue(any()) }
        verify(exactly = 1) { context.proceed() }
    }

    @Test
    fun `test intercept with CurrentUser parameter but no authenticated user`() {
        // Given
        val paramName = "userInfo"

        // Mock parameter with @CurrentUser annotation
        val annotationMetadata = mockk<AnnotationMetadata>()
        every { annotationMetadata.hasAnnotation(CurrentUser::class.java) } returns true

        val userInfoParam = mockk<MutableArgumentValue<UserInfo>>()
        every { userInfoParam.annotationMetadata } returns annotationMetadata

        // Set up parameters map
        val parameters = mapOf(paramName to userInfoParam)

        every { context.parameters } returns parameters
        every { securityService.authentication } returns Optional.empty()

        // When/Then
        val exception =
            assertThrows<SecurityException> {
                interceptor.intercept(context)
            }
        assertEquals("No authenticated user found", exception.message)

        // Verify
        verify(exactly = 1) { context.parameters }
        verify(exactly = 1) { securityService.authentication }
        verify(exactly = 0) { context.proceed() }
    }

    @Test
    fun `test intercept with multiple CurrentUser parameters`() {
        // Given
        val param1Name = "userInfo1"
        val param2Name = "userInfo2"
        val userId = "test-user-id"
        val username = "testuser"
        val email = "test@example.com"
        val attributes =
            mapOf(
                "sub" to userId,
                "username" to username,
                "email" to email,
            )
        val expectedResult = "result"

        // Mock parameters with @CurrentUser annotation
        val annotationMetadata = mockk<AnnotationMetadata>()
        every { annotationMetadata.hasAnnotation(CurrentUser::class.java) } returns true

        // Create mutable argument values
        val userInfoParam1 = mockk<MutableArgumentValue<UserInfo>>()
        val userInfoParam2 = mockk<MutableArgumentValue<UserInfo>>()
        val capturedUserInfo1 = slot<UserInfo>()
        val capturedUserInfo2 = slot<UserInfo>()

        every { userInfoParam1.type } returns UserInfo::class.java
        every { userInfoParam1.annotationMetadata } returns annotationMetadata
        every { userInfoParam1.setValue(capture(capturedUserInfo1)) } returns Unit

        every { userInfoParam2.type } returns UserInfo::class.java
        every { userInfoParam2.annotationMetadata } returns annotationMetadata
        every { userInfoParam2.setValue(capture(capturedUserInfo2)) } returns Unit

        // Set up parameters map
        val parameters =
            mapOf(
                param1Name to userInfoParam1,
                param2Name to userInfoParam2,
            )

        every { context.parameters } returns parameters
        every { securityService.authentication } returns Optional.of(authentication)
        every { authentication.attributes } returns attributes
        every { context.proceed() } returns expectedResult

        // When
        val result = interceptor.intercept(context)

        // Then
        assertEquals(expectedResult, result)
        assertEquals(userId, capturedUserInfo1.captured.id)
        assertEquals(username, capturedUserInfo1.captured.username)
        assertEquals(email, capturedUserInfo1.captured.email)
        assertEquals(userId, capturedUserInfo2.captured.id)
        assertEquals(username, capturedUserInfo2.captured.username)
        assertEquals(email, capturedUserInfo2.captured.email)

        // Verify
        verify(exactly = 1) { context.parameters }
        verify(exactly = 1) { securityService.authentication }
        verify(exactly = 1) { userInfoParam1.setValue(any()) }
        verify(exactly = 1) { userInfoParam2.setValue(any()) }
        verify(exactly = 1) { context.proceed() }
    }
}
