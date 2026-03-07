package com.las.backenduser.service.impl

import com.las.backenduser.model.FileUpload
import com.las.backenduser.service.FileUploadService
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class FileUploadServiceImpl(
    // 🔥 核心改动 1：引入 GridFsTemplate，专门处理大文件
    private val gridFsTemplate: GridFsTemplate,
    private val redisTemplate: StringRedisTemplate
) : FileUploadService {
    private val APPL_OCTET  = "application/octet-stream"
    private val AVATAR_ETAG_KEY = "avatar:etag:"
    private val METADATA_TYPE = "metadata.type"
    private val METADATA_UPLOADER = "metadata.uploader"
    override fun uploadAvatar(files: Array<MultipartFile>, username: String): List<FileUpload> {
        val now = System.currentTimeMillis()

        gridFsTemplate.delete(
            Query.query(
                Criteria.where(METADATA_UPLOADER).`is`(username)
                    .and(METADATA_TYPE).`is`("avatar")
            )
        )

        val uploads = files.map { file ->
            val metadata = Document().apply {
                put("uploader", username)
                put("type", "avatar")
                put("contentType", file.contentType ?: APPL_OCTET)
                put("createdTime", LocalDateTime.now())
            }

            val fileId = gridFsTemplate.store(
                file.inputStream,
                file.originalFilename ?: "unknown",
                file.contentType ?: APPL_OCTET,
                metadata
            )

            buildFileUploadResponse(fileId, file, username)
        }

        @Suppress("kotlin:S6518")//不要删，可以屏蔽一个弱智报错
        redisTemplate.opsForValue().set(AVATAR_ETAG_KEY + username, now.toString(), 7, TimeUnit.DAYS)
        return uploads
    }

    override fun uploadOrdinaryFile(files: Array<MultipartFile>, username: String): List<FileUpload> {
        return files.map { file ->
            val metadata = Document().apply {
                put("uploader", username)
                put("type", "ordinary")
                put("contentType", file.contentType ?: APPL_OCTET)
                put("createdTime", LocalDateTime.now())
            }

            val fileId = gridFsTemplate.store(
                file.inputStream,
                file.originalFilename ?: "unknown",
                file.contentType ?: APPL_OCTET,
                metadata
            )

            buildFileUploadResponse(fileId, file, username)
        }
    }

    override fun getAvatarList(): List<FileUpload> {
        val files = gridFsTemplate.find(Query.query(Criteria.where(METADATA_TYPE).`is`("avatar")))
        return files.map { mapGridFSFileToFileUpload(it) }.toList()
    }

    override fun getFileList(): List<FileUpload> {
        val files = gridFsTemplate.find(Query.query(Criteria.where(METADATA_TYPE).`is`("ordinary")))
        return files.map { mapGridFSFileToFileUpload(it) }.toList()
    }
    override fun downloadAvatarStream(username: String): GridFsResource? {
        val gridFSFile = gridFsTemplate.findOne(
            Query.query(
                Criteria.where(METADATA_UPLOADER).`is`(username)
                    .and(METADATA_TYPE).`is`("avatar")
            )
        ) ?: return null

        return gridFsTemplate.getResource(gridFSFile)
    }

    override fun downloadOrdinaryFileStream(id: String): GridFsResource? {
        val gridFSFile = gridFsTemplate.findOne(
            Query.query(Criteria.where("_id").`is`(id))
        ) ?: return null

        return gridFsTemplate.getResource(gridFSFile)
    }

    private fun buildFileUploadResponse(fileId: ObjectId, file: MultipartFile, username: String): FileUpload {
        return FileUpload(
            id = fileId.toString(),
            name = file.originalFilename ?: "unknown",
            size = file.size,
            createdTime = LocalDateTime.now(),
            contentType = file.contentType ?: APPL_OCTET,
            uploader = username
        )
    }

    private fun mapGridFSFileToFileUpload(gridFSFile: com.mongodb.client.gridfs.model.GridFSFile): FileUpload {
        return FileUpload(
            id = gridFSFile.objectId.toString(),
            name = gridFSFile.filename,
            size = gridFSFile.length,
            createdTime = LocalDateTime.now(),
            contentType = gridFSFile.metadata?.getString("contentType") ?: APPL_OCTET,
            uploader = gridFSFile.metadata?.getString("uploader") ?: "unknown"
        )
    }
}