package com.las.backenduser.model

import java.time.LocalDateTime

data class FileUpload(
    val id: String? = null,
    val name: String,
    val size: Long,
    val createdTime: LocalDateTime,
    val contentType: String,
    val uploader: String
)