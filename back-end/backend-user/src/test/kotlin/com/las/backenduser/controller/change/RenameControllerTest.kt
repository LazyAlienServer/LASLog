package com.las.backenduser.controller.change

import com.las.backenduser.service.LoginService
import com.las.backenduser.service.RenameService
import com.las.backenduser.utils.jwt.JwtUtils
import com.las.backenduser.websocket.WsClientService
import io.jsonwebtoken.Claims
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(RenameController::class)
class RenameControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var renameService: RenameService

    @MockitoBean
    lateinit var jwtUtils: JwtUtils

    @MockitoBean
    lateinit var loginService: LoginService

    @MockitoBean
    lateinit var wsClientService: WsClientService

    private fun <T> any(type: Class<T>): T = org.mockito.Mockito.any(type)

    @Test
    fun `test rename no auth`() {
        mockMvc.perform(
            post("/change/rename")
                .param("newName", "newName")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.msg").value("未登录"))
    }

    @Test
    fun `test rename exception`() {
        val token = "valid_token"

        `when`(jwtUtils.parseToken(token)).thenThrow(RuntimeException("Unexpected error"))

        mockMvc.perform(
            post("/change/rename")
                .header("Authorization", "Bearer $token")
                .param("newName", "newName")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.msg").value("操作失败: Unexpected error"))
    }

    @Test
    fun `test rename success`() {
        val token = "valid_token"
        val uuid = "user-uuid"
        val newName = "NewName"
        val claims = mockClaims(uuid, Date())

        `when`(jwtUtils.parseToken(token)).thenReturn(claims)
        `when`(loginService.isKickedOut(anyString(), any(Date::class.java))).thenReturn(false)
        `when`(renameService.rename(uuid, newName)).thenReturn(com.las.backenduser.utils.result.ResultUtil.result(200, "用户名修改成功"))

        mockMvc.perform(
            post("/change/rename")
                .header("Authorization", "Bearer $token")
                .param("newName", newName)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("用户名修改成功"))
    }

    @Test
    fun `test rename failed when kicked out`() {
        val token = "valid_token"
        val uuid = "user-uuid"
        val newName = "NewName"
        val claims = mockClaims(uuid, Date())

        `when`(jwtUtils.parseToken(token)).thenReturn(claims)
        `when`(loginService.isKickedOut(anyString(), any(Date::class.java))).thenReturn(true)

        mockMvc.perform(
            post("/change/rename")
                .header("Authorization", "Bearer $token")
                .param("newName", newName)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.msg").value("凭证已失效，请重新登录"))
    }

    private fun mockClaims(subject: String, issuedAt: Date): Claims {
        val claims = mock(Claims::class.java)
        `when`(claims.subject).thenReturn(subject)
        `when`(claims.issuedAt).thenReturn(issuedAt)
        return claims
    }
}
