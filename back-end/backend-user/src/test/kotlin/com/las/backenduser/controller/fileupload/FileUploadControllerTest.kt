package com.las.backenduser.controller.fileupload

import com.las.backenduser.service.FileUploadService
import com.las.backenduser.service.LoginService
import com.las.backenduser.websocket.WsClientService
import com.las.backenduser.utils.jwt.JwtUtils
import io.jsonwebtoken.Claims
import java.util.Date
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.multipart.MultipartFile
import org.springframework.data.redis.core.StringRedisTemplate // 🔥 新增导入

@WebMvcTest(FileUploadController::class)
class FileUploadControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var wsClientService: WsClientService

    @MockitoBean
    lateinit var fileUploadService: FileUploadService

    @MockitoBean
    lateinit var loginService: LoginService

    @MockitoBean
    lateinit var jwtUtils: JwtUtils

    @MockitoBean
    lateinit var redisTemplate: StringRedisTemplate

    // 解决 Kotlin 泛型擦除导致 any() 报错的小工具
    private fun <T> any(type: Class<T>): T = Mockito.any(type)

    @Test
    fun `test uploadAvatar api should return 200 and correct url`() {
        val username = "sunyinuo"
        val token = "valid_token"
        val mockFile = MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            "test content".toByteArray()
        )

        // Mock JWT
        val claims = Mockito.mock(Claims::class.java)
        `when`(claims.subject).thenReturn(username)
        `when`(claims.issuedAt).thenReturn(Date())
        `when`(jwtUtils.parseToken(anyString())).thenReturn(claims)

        // 模拟 service 层方法 (注意这里的 any 匹配的是 Array)
        `when`(fileUploadService.uploadAvatar(any(Array<MultipartFile>::class.java), anyString()))
            .thenReturn(emptyList())

        // 模拟 loginService 校验未被踢出
        `when`(loginService.isKickedOut(anyString(), any(Date::class.java))).thenReturn(false)

        mockMvc.perform(
            multipart("/fileupload/avatar")
                .file(mockFile)
                .header("Authorization", token)
        )
            .andExpect(status().isOk) // HTTP 状态码 200
            .andExpect(jsonPath("$.code").value(200)) // 自定义业务状态码 200
            .andExpect(jsonPath("$.msg").value("头像上传成功"))
            // 注意：MockMvc 默认跑在 localhost:80 环境下，所以返回的 URL 是这个
            .andExpect(jsonPath("$.data[0]").value("http://localhost:80/fileupload/avatar/get/sunyinuo"))
    }

    @Test
    fun `test uploadAvatar with empty file should return 400`() {
        val username = "sunyinuo"
        // 构造一个大小为 0 的空文件
        val emptyFile = MockMultipartFile("file", "empty.png", "image/png", ByteArray(0))

        mockMvc.perform(
            multipart("/fileupload/avatar")
                .file(emptyFile)
                .header("Authorization", "token")
        )
            .andExpect(status().isOk) // 你的 Controller 是 return ResultUtil.result(...)，所以 HTTP 仍然是 200
            .andExpect(jsonPath("$.code").value(400)) // 但内部业务 code 是 400
            .andExpect(jsonPath("$.msg").value("上传失败：请至少选择一个文件"))
    }
}