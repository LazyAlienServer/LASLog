package com.las.backenduser.model

import org.bson.types.Binary
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.LocalDateTime

@Document
data class FileUpload(
    @Id
    val id: String? = null,
    var name: String,
    var size: Long,
    var createdTime: LocalDateTime = LocalDateTime.now(),
    var content: Binary,
    var contentType: String,
    var uploader: String//PLEASE USE USERNAME!!!!!! PLEASEEEEEEEE
): Serializable