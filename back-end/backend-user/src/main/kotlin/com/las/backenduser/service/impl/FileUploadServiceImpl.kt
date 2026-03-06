package com.las.backenduser.service.impl

import com.las.backenduser.model.FileUpload
import com.las.backenduser.service.FileUploadService
import org.bson.types.Binary
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@Service
class FileUploadServiceImpl(
    private val mongoTemplate: MongoTemplate
) : FileUploadService {

    private val AVATAR_COLLECTION = "avatar"
    private val FILE_COLLECTION = "upload_files"


    override fun uploadAvatar(files: Array<MultipartFile>, username: String): List<FileUpload> {
        val uploads = files.map { file ->
            FileUpload(
                id = username,
                name = file.originalFilename ?: "unknown",
                size = file.size,
                createdTime = LocalDateTime.now(),
                content = Binary(file.bytes),
                contentType = file.contentType ?: "application/octet-stream",
                uploader = username
            )
        }

        uploads.forEach { mongoTemplate.save(it, AVATAR_COLLECTION) }
        return uploads
    }

    override fun uploadOrdinaryFile(files: Array<MultipartFile>, username: String): List<FileUpload> {
        val uploads = files.map { file ->
            FileUpload(
                name = file.originalFilename ?: "unknown",
                size = file.size,
                createdTime = LocalDateTime.now(),
                content = Binary(file.bytes),
                contentType = file.contentType ?: "application/octet-stream",
                uploader = username
            )
        }
        return mongoTemplate.insert(uploads, FILE_COLLECTION).toList()
    }

    override fun getAvatar(username: String): FileUpload? {
        return mongoTemplate.findById(username, FileUpload::class.java, AVATAR_COLLECTION)
    }

    override fun getOrdinaryFile(id: String): FileUpload? {
        return mongoTemplate.findById(id, FileUpload::class.java, FILE_COLLECTION)
    }

    override fun getAvatarList(): List<FileUpload> {
        return mongoTemplate.findAll(FileUpload::class.java, AVATAR_COLLECTION)
    }

    override fun getFileList(): List<FileUpload> {
        return mongoTemplate.findAll(FileUpload::class.java, FILE_COLLECTION)
    }
}