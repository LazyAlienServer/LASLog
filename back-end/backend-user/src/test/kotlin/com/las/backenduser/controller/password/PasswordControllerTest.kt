package com.las.backenduser.controller.password

import com.las.backenduser.controller.change.ChangePasswordController
import com.las.backenduser.service.LoginService
import com.las.backenduser.service.PasswordService
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

@WebMvcTest(ChangePasswordController::class)
class PasswordControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var passwordService: PasswordService

    @MockitoBean
    lateinit var jwtUtils: JwtUtils

    @MockitoBean
    lateinit var loginService: LoginService

    @MockitoBean
    lateinit var wsClientService: WsClientService

    private fun <T> any(type: Class<T>): T = org.mockito.Mockito.any(type)

    @Test
    fun `test changePassword no auth`() {
        mockMvc.perform(
            post("/change/password")
                .param("oldPass", "old")
                .param("newPass", "new")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.msg").value("未登录"))
    }

    @Test
    fun `test changePassword exception`() {
        val token = "valid_token"
        val uuid = "user-uuid"

        // Mock exception
        `when`(jwtUtils.parseToken(token)).thenThrow(RuntimeException("Unexpected error"))

        mockMvc.perform(
            post("/change/password")
                .header("Authorization", "Bearer $token")
                .param("oldPass", "old")
                .param("newPass", "new")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("操作失败: Unexpected error"))
    }

    @Test
    fun `test changePassword success`() {
        val token = "valid_token"
        val uuid = "user-uuid"
        val oldPass = "oldPass"
        val newPass = "newPass"

        val claims = mock(Claims::class.java)
        `when`(claims.subject).thenReturn(uuid)
        `when`(claims.issuedAt).thenReturn(Date())

        `when`(jwtUtils.parseToken(token)).thenReturn(claims)
        `when`(loginService.isKickedOut(anyString(), any(Date::class.java))).thenReturn(false)
        `when`(passwordService.changePassword(uuid, oldPass, newPass)).thenReturn(com.las.backenduser.utils.result.ResultUtil.result(200, "密码修改成功"))

        mockMvc.perform(
            post("/change/password")
                .header("Authorization", "Bearer $token")
                .param("oldPass", oldPass)
                .param("newPass", newPass)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("密码修改成功"))
    }

    @Test
    fun `test changePassword failed when kicked out`() {
        val token = "valid_token"
        val uuid = "user-uuid"
        val oldPass = "oldPass"
        val newPass = "newPass"

        val claims = mock(Claims::class.java)
        `when`(claims.subject).thenReturn(uuid)
        `when`(claims.issuedAt).thenReturn(Date())

        `when`(jwtUtils.parseToken(token)).thenReturn(claims)
        `when`(loginService.isKickedOut(anyString(), any(Date::class.java))).thenReturn(true)

        mockMvc.perform(
            post("/change/password")
                .header("Authorization", "Bearer $token")
                .param("oldPass", oldPass)
                .param("newPass", newPass)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.msg").value("凭证已失效，请重新登录"))
    }
}
