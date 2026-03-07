package com.las.backenduser.service.impl

import com.mongodb.client.gridfs.model.GridFSFile
import org.bson.Document
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.mock.web.MockMultipartFile
import java.io.InputStream
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
class FileUploadServiceImplTest {

    // 🔥 1. 替换为最新的依赖：GridFsTemplate 和 StringRedisTemplate
    @Mock
    lateinit var gridFsTemplate: GridFsTemplate

    @Mock
    lateinit var redisTemplate: StringRedisTemplate

    // 用于 Mock Redis 的 opsForValue() 操作
    @Mock
    lateinit var valueOperations: ValueOperations<String, String>

    @InjectMocks
    lateinit var fileUploadService: FileUploadServiceImpl

    @Test
    fun `test uploadAvatar should store file to GridFS and set Redis ETag`() {
        // 1. 准备测试数据
        val username = "sunyinuo"
        val mockFile = MockMultipartFile(
            "file",
            "my_avatar.png",
            "image/png",
            "dummy image bytes".toByteArray()
        )
        // 模拟 GridFS 存入后返回的 ObjectId
        val expectedObjectId = ObjectId()

        // 2. 配置 Mock 行为
        // 模拟 Redis opsForValue() 的调用
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)

        // 模拟 GridFS 的 store 方法，返回我们预设的 ObjectId
        `when`(
            gridFsTemplate.store(
                any(InputStream::class.java),
                anyString(),
                anyString(),
                any(Document::class.java)
            )
        ).thenReturn(expectedObjectId)

        // 3. 执行测试方法
        val result = fileUploadService.uploadAvatar(arrayOf(mockFile), username)

        // 4. 断言返回的 FileUpload 对象是否正确
        assertEquals(1, result.size, "应该只返回一个文件记录")
        // 现在的 ID 应该是 GridFS 生成的 ObjectId 字符串，不再是 username 了
        assertEquals(expectedObjectId.toString(), result[0].id, "文件 ID 应该等于 GridFS 返回的 ObjectId")
        assertEquals("my_avatar.png", result[0].name, "文件名应该正确解析")
        assertEquals(username, result[0].uploader, "上传者应该绑定正确")

        // 5. 验证依赖的方法是否被正确调用
        // 验证是否调用了 delete 来删除旧头像
        verify(gridFsTemplate).delete(any(Query::class.java))
        // 验证 Redis ETag 是否被成功设置 (缓存 7 天)
        verify(valueOperations).set(startsWith("avatar:etag:$username"), anyString(), eq(7L), eq(TimeUnit.DAYS))
    }

    @Test
    fun `test downloadAvatarStream should return GridFsResource`() {
        // 1. 准备假数据
        val username = "sunyinuo"

        // 模拟一个 GridFSFile 和 GridFsResource
        val mockGridFSFile = org.mockito.Mockito.mock(GridFSFile::class.java)
        val mockResource = org.mockito.Mockito.mock(GridFsResource::class.java)

        // 告诉 Mockito：当使用 Query 去 findOne 时，返回假文件
        `when`(gridFsTemplate.findOne(any(Query::class.java))).thenReturn(mockGridFSFile)
        // 当获取 resource 时，返回假的 Resource 流对象
        `when`(gridFsTemplate.getResource(mockGridFSFile)).thenReturn(mockResource)

        // 2. 执行方法 (我们测最新的下载流方法)
        val result = fileUploadService.downloadAvatarStream(username)

        // 3. 断言
        assertNotNull(result, "应该能获取到文件的下载流")

        // 4. 验证 findOne 是否被触发
        verify(gridFsTemplate).findOne(any(Query::class.java))
    }

    // --- 解决 Kotlin 中 Mockito any() 报错的小工具方法 ---
    private fun <T> any(type: Class<T>): T = org.mockito.Mockito.any(type)
}