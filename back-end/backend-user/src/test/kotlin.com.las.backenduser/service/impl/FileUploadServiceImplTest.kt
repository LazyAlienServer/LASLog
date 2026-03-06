package com.las.backenduser.service.impl

import com.las.backenduser.model.FileUpload
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.mock.web.MockMultipartFile

@ExtendWith(MockitoExtension::class)
class FileUploadServiceImplTest {

    @Mock
    lateinit var mongoTemplate: MongoTemplate // 伪造一个 MongoTemplate

    @InjectMocks
    lateinit var fileUploadService: FileUploadServiceImpl // 注入伪造的依赖

    @Test
    fun `test uploadAvatar should save to admin-avatar collection`() {
        // 1. 准备测试数据
        val username = "sunyinuo"
        val mockFile = MockMultipartFile(
            "file",
            "my_avatar.png",
            "image/png",
            "dummy image bytes".toByteArray()
        )

        // 2. 执行测试方法
        val result = fileUploadService.uploadAvatar(arrayOf(mockFile), username)

        // 3. 断言 (验证结果是否符合预期)
        assertEquals(1, result.size, "应该只返回一个文件记录")
        assertEquals(username, result[0].id, "头像的 ID 必须被强制设为 username")
        assertEquals("my_avatar.png", result[0].name, "文件名应该正确解析")
        assertEquals(username, result[0].uploader, "上传者应该绑定正确")

        // 4. 验证 MongoTemplate 的 save 方法是否被调用了一次，并且是存入 "admin/avatar" 表
        verify(mongoTemplate, times(1)).save(any(FileUpload::class.java), eq("admin/avatar"))
    }

    @Test
    fun `test getAvatar should return file from mongo`() {
        // 1. 准备假数据
        val username = "sunyinuo"
        val mockFile = FileUpload(
            id = username,
            name = "test.png",
            size = 100,
            content = org.bson.types.Binary(byteArrayOf(1, 2, 3)),
            contentType = "image/png",
            uploader = username
        )

        // 告诉 Mockito：当有人调用 findById 查这个 username 时，返回这个假文件
        `when`(mongoTemplate.findById(eq(username), eq(FileUpload::class.java), eq("admin/avatar")))
            .thenReturn(mockFile)

        // 2. 执行方法
        val result = fileUploadService.getAvatar(username)

        // 3. 断言
        assertNotNull(result, "应该能查到头像")
        assertEquals("test.png", result?.name)
    }

    // 解决 Kotlin 中 Mockito any() 报错的小工具方法
    private fun <T> any(type: Class<T>): T = Mockito.any(type)
}