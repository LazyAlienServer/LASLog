package com.las.backenduser.controller.fileupload

import com.las.backenduser.service.FileUploadService
import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultUtil
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MultipartFile
import java.io.Serializable

@RestController
@RequestMapping("/fileupload")
class FileUploadController(
    private val fileUploadService: FileUploadService,
    private val redisTemplate: StringRedisTemplate
) {

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @RequestHeader("username") username: String,
        @RequestPart("file") files: List<MultipartFile>,
        @Parameter(hidden = true) request: HttpServletRequest
    ): Result<out Serializable> {

        // 校验逻辑 (List 的用法与 Array 一致)
        if (files.isEmpty() || files[0].isEmpty) {
            return ResultUtil.result(400, "上传失败：请至少选择一个文件")
        }

        return try {
            fileUploadService.uploadAvatar(files.toTypedArray(), username)

            val baseUrl = getBaseUrl(request)
            val urls = arrayListOf("$baseUrl/avatar/get/$username")
            ResultUtil.result(200, urls, "头像上传成功")
        } catch (e: Exception) {
            ResultUtil.result(500, "头像上传异常: ${e.message}")
        }
    }

    @PostMapping("/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestHeader("username") username: String,
        @RequestPart("file") files: List<MultipartFile>,
        @Parameter(hidden = true) request: HttpServletRequest
    ): Result<out Serializable> {

        if (files.isEmpty() || files[0].isEmpty) {
            return ResultUtil.result(400, "上传失败：请至少选择一个文件")
        }

        return try {
            val savedFiles = fileUploadService.uploadOrdinaryFile(files.toTypedArray(), username)
            val baseUrl = getBaseUrl(request)
            val urls = savedFiles.map { "$baseUrl/file/get/${it.id}" }
            ResultUtil.result(200, ArrayList(urls), "文件上传成功")
        } catch (e: Exception) {
            ResultUtil.result(500, "文件上传异常: ${e.message}")
        }
    }

    @GetMapping("/avatar/get/{username}")
    fun getAvatar(
        @PathVariable username: String,
        webRequest: WebRequest
    ): ResponseEntity<out Serializable> {

        val redisEtag = redisTemplate.opsForValue()["avatar:etag:$username"]
        if (redisEtag != null && webRequest.checkNotModified(redisEtag)) {
            return ResponseEntity.status(304).build<Nothing>() // ✅ 修复泛型推断
        }

        return try {
            val file = fileUploadService.getAvatar(username)
                ?: return ResponseEntity.status(404).body(ResultUtil.result(404, "头像不存在"))

            val currentEtag = redisEtag ?: file.createdTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli().toString()
            val mediaType = runCatching { MediaType.parseMediaType(file.contentType) }
                .getOrDefault(MediaType.APPLICATION_OCTET_STREAM)

            ResponseEntity.ok()
                .contentType(mediaType)
                .eTag(currentEtag)
                .body(file.content.data)

        } catch (e: Exception) {
            ResponseEntity.status(500).body(ResultUtil.result(500, "获取头像异常: ${e.message}"))
        }
    }

    @GetMapping("/file/get/{id}")
    fun getFile(
        @PathVariable id: String,
        webRequest: WebRequest
    ): ResponseEntity<out Serializable> {

        val redisEtag = redisTemplate.opsForValue()["file:etag:$id"]
        if (redisEtag != null && webRequest.checkNotModified(redisEtag)) {
            return ResponseEntity.status(304).build<Nothing>()
        }

        return try {
            val file = fileUploadService.getOrdinaryFile(id)
                ?: return ResponseEntity.status(404).body(ResultUtil.result(404, "文件不存在"))

            val currentEtag = redisEtag ?: file.createdTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli().toString()
            val mediaType = runCatching { MediaType.parseMediaType(file.contentType) }
                .getOrDefault(MediaType.APPLICATION_OCTET_STREAM)

            val disposition = "inline; filename=\"${java.net.URLEncoder.encode(file.name, "UTF-8")}\""

            ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition) // 设置文件名
                .eTag(currentEtag)
                .body(file.content.data)

        } catch (e: Exception) {
            ResponseEntity.status(500).body(ResultUtil.result(500, "获取文件异常: ${e.message}"))
        }
    }

    @GetMapping("/avatar/list")
    fun getAvatarList(request: HttpServletRequest): Result<out Serializable> {
        return try {
            val avatars = fileUploadService.getAvatarList()
            val baseUrl = getBaseUrl(request)

            val resultList = avatars.map { file ->
                mapOf(
                    "uploader" to file.uploader,
                    "name" to file.name,
                    "uploadTime" to file.createdTime.toString(),
                    "size" to file.size,
                    "avatarUrl" to "$baseUrl/avatar/get/${file.uploader}"
                )
            }
            ResultUtil.result(200, ArrayList(resultList), "获取全局头像列表成功")
        } catch (e: Exception) {
            ResultUtil.result(500, "获取列表异常: ${e.message}")
        }
    }

    @GetMapping("/file/list")
    fun getFileList(request: HttpServletRequest): Result<out Serializable> {
        return try {
            val files = fileUploadService.getFileList()
            val baseUrl = getBaseUrl(request)

            val resultList = files.map { file ->
                mapOf(
                    "id" to (file.id ?: ""),
                    "uploader" to file.uploader,
                    "name" to file.name,
                    "uploadTime" to file.createdTime.toString(),
                    "size" to file.size,
                    "downloadUrl" to "$baseUrl/file/get/${file.id}"
                )
            }
            ResultUtil.result(200, ArrayList(resultList), "获取全局文件列表成功")
        } catch (e: Exception) {
            ResultUtil.result(500, "获取列表异常: ${e.message}")
        }
    }

    private fun getBaseUrl(request: HttpServletRequest): String {
        val scheme = request.scheme
        val serverName = request.serverName
        val serverPort = request.serverPort
        val contextPath = request.contextPath
        return "$scheme://$serverName:$serverPort$contextPath/fileupload"
    }
}