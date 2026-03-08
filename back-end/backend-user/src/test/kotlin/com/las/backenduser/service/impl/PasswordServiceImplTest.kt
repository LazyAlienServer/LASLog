package com.las.backenduser.service.impl

import com.las.backenduser.mapper.UserMapper
import com.las.backenduser.model.User
import com.las.backenduser.service.LoginService
import com.las.backenduser.utils.result.ResultEnum
import com.las.backenduser.utils.salt.Salt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PasswordServiceImplTest {

    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var loginService: LoginService

    @InjectMocks
    lateinit var passwordService: PasswordServiceImpl

    private lateinit var user: User
    private val saltCode = "testSalt"
    private val encryptedPass = "encryptedPass"

    @Test
    fun `changePassword should return fail if user not found`() {
        `when`(userMapper.selectById("test-uuid")).thenReturn(null)

        val result = passwordService.changePassword("test-uuid", "oldPass", "newPass")

        assertEquals(ResultEnum.FAIL.code, result.code)
        assertEquals("用户不存在", result.msg)
    }

    @Test
    fun `changePassword should return fail if old password incorrect`() {
        user = User()
        user.uuid = "test-uuid"
        user.salt = saltCode
        user.password = encryptedPass

        `when`(userMapper.selectById("test-uuid")).thenReturn(user)

        // Mock static method call for password check
        val saltMock: MockedStatic<Salt> = mockStatic(Salt::class.java)
        try {
            saltMock.`when`<String> { Salt.salt("wrongPass", saltCode) }.thenReturn("wrongEncrypted")

            val result = passwordService.changePassword("test-uuid", "wrongPass", "newPass")

            assertEquals(ResultEnum.FAIL.code, result.code)
            assertEquals("旧密码错误", result.msg)
        } finally {
            saltMock.close()
        }
    }

    @Test
    fun `changePassword should update password and kick out user if success`() {
        user = User()
        user.uuid = "test-uuid"
        user.salt = saltCode
        user.password = encryptedPass

        `when`(userMapper.selectById("test-uuid")).thenReturn(user)

        val saltMock: MockedStatic<Salt> = mockStatic(Salt::class.java)
        try {
            // Mock old password check
            saltMock.`when`<String> { Salt.salt("oldPass", saltCode) }.thenReturn(encryptedPass)

            // Mock new password generation
            val newPassObj = com.las.backenduser.model.Password("newEncrypted", "newSalt")
            saltMock.`when`<com.las.backenduser.model.Password> { Salt.salt("newPass") }.thenReturn(newPassObj)

            val result = passwordService.changePassword("test-uuid", "oldPass", "newPass")

            assertEquals(ResultEnum.SUCCESS.code, result.code)
            assertEquals("密码修改成功，请重新登录", result.msg)

            verify(userMapper).updateById(user)
            verify(loginService).kickOutByUuid("test-uuid")

            assertEquals("newEncrypted", user.password)
            assertEquals("newSalt", user.salt)
        } finally {
            saltMock.close()
        }
    }
}
