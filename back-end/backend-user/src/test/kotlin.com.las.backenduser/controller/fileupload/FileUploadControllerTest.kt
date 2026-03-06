package com.las.backenduser.controller.fileupload

import com.las.backenduser.service.FileUploadService
import com.las.backenduser.websocket.WsClientService
import org.junit.jupiter.api.Test
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

@WebMvcTest(FileUploadController::class)
class FileUploadControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var fileUploadService: FileUploadService

    @MockitoBean
    lateinit var wsClientService: WsClientService

    private fun <T> any(type: Class<T>): T = Mockito.any(type)

    @Test
    fun `test uploadAvatar api should return 200 and correct url`() {
        val username = "sunyinuo"
        val mockFile = MockMultipartFile(
            "file",
            "avatar.png",
            "image/png",
            "test content".toByteArray()
        )

        `when`(fileUploadService.uploadAvatar(any(Array<MultipartFile>::class.java), anyString()))
            .thenReturn(emptyList())

        mockMvc.perform(
            multipart("/fileupload/avatar")
                .file(mockFile)
                .header("token", username)
                .header("username", username)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("头像上传成功"))
            .andExpect(jsonPath("$.data[0]").value("http://localhost:80/fileupload/avatar/get/sunyinuo"))
    }

    @Test
    fun `test uploadAvatar with empty file should return 400`() {
        val username = "sunyinuo"
        val emptyFile = MockMultipartFile("file", "empty.png", "image/png", ByteArray(0))

        mockMvc.perform(
            multipart("/fileupload/avatar")
                .file(emptyFile)
                .header("token", username)
                .header("username", username)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.msg").value("上传失败：请至少选择一个文件"))
    }
}