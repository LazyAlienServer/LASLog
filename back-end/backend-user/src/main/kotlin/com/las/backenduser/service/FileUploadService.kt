package com.las.backenduser.service

import com.las.backenduser.model.FileUpload
import org.springframework.data.mongodb.gridfs.GridFsResource
import org.springframework.web.multipart.MultipartFile

interface FileUploadService {
    fun uploadAvatar(files: Array<MultipartFile>, username: String): List<FileUpload>
    fun uploadOrdinaryFile(files: Array<MultipartFile>, username: String): List<FileUpload>
    fun getAvatarList(): List<FileUpload>
    fun getFileList(): List<FileUpload>
    fun downloadAvatarStream(username: String): GridFsResource?
    fun downloadOrdinaryFileStream(id: String): GridFsResource?
}