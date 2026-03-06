package com.las.backenduser.service

import com.las.backenduser.model.FileUpload
import org.springframework.web.multipart.MultipartFile

interface FileUploadService {
    fun uploadAvatar(files: Array<MultipartFile>, username: String): List<FileUpload>
    fun uploadOrdinaryFile(files: Array<MultipartFile>, username: String): List<FileUpload>
    fun getAvatar(username: String): FileUpload?
    fun getOrdinaryFile(id: String): FileUpload?
    fun getAvatarList(): List<FileUpload>
    fun getFileList(): List<FileUpload>
}