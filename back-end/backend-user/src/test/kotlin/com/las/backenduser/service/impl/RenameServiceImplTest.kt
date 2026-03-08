package com.las.backenduser.service.impl

import com.las.backenduser.mapper.UserMapper
import com.las.backenduser.model.User
import com.las.backenduser.utils.result.ResultEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentMatchers.any

@ExtendWith(MockitoExtension::class)
class RenameServiceImplTest {

    @Mock
    lateinit var userMapper: UserMapper

    @InjectMocks
    lateinit var renameService: RenameServiceImpl

    @Test
    fun `rename should return fail if new name is blank`() {
        val result = renameService.rename("uuid", "")
        assertEquals(403, result.code)
        assertEquals("用户名不能为空", result.msg)
    }

    @Test
    fun `rename should return fail if username already exists`() {
        `when`(userMapper.selectCount(any())).thenReturn(1L)

        val result = renameService.rename("uuid", "existsName")

        assertEquals(403, result.code)
        assertEquals("用户名已存在", result.msg)
    }

    @Test
    fun `rename should return fail if user not found`() {
        `when`(userMapper.selectCount(any())).thenReturn(0L)
        `when`(userMapper.selectById("uuid")).thenReturn(null)

        val result = renameService.rename("uuid", "validName")

        assertEquals(ResultEnum.FAIL.code, result.code)
        assertEquals("用户不存在", result.msg)
    }

    @Test
    fun `rename should success if all checks pass`() {
        val user = User()
        user.uuid = "uuid"
        user.username = "oldName"

        `when`(userMapper.selectCount(any())).thenReturn(0L)
        `when`(userMapper.selectById("uuid")).thenReturn(user)

        val result = renameService.rename("uuid", "newName")

        assertEquals(ResultEnum.SUCCESS.code, result.code)
        assertEquals("用户名修改成功", result.msg)

        verify(userMapper).updateById(user)
        assertEquals("newName", user.username)
    }
}

