package com.las.backenduser.controller.fileupload

import com.las.backenduser.exception.UnauthorizedException
import com.las.backenduser.service.FileUploadService
import com.las.backenduser.service.LoginService
import com.las.backenduser.utils.jwt.JwtUtils
import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultEnum
import com.las.backenduser.utils.result.ResultUtil
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.InputStreamResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MultipartFile
import java.io.Serializable
import java.net.URLEncoder

private const val BEARER_PREFIX = "Bearer "
private const val MSG_CREDENTIALS_EXPIRED = "凭证已失效，请重新登录"

@Suppress("S1192")
@RestController
@RequestMapping("/fileupload")
class FileUploadController(
    private val fileUploadService: FileUploadService,
    private val redisTemplate: StringRedisTemplate,
    private val jwtUtils: JwtUtils,
    private val loginService: LoginService
) {

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAvatar(
        @Parameter(hidden = true)
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        @RequestPart("file") files: List<MultipartFile>,
        @Parameter(hidden = true) request: HttpServletRequest
    ): Result<out Serializable> {

        if (authorization.isNullOrBlank()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.code, "未登录")
        }

        if (files.isEmpty() || files[0].isEmpty) {
            return ResultUtil.result(400, "上传失败：请至少选择一个文件")
        }

        return try {
            val token = if (authorization.startsWith(BEARER_PREFIX)) authorization.substring(7) else authorization
            val claims = jwtUtils.parseToken(token)
            val uuid = claims.subject

            if (loginService.isKickedOut(uuid, claims.issuedAt)) {
                return ResultUtil.result(ResultEnum.UNAUTHORIZED.code, MSG_CREDENTIALS_EXPIRED)
            }

            fileUploadService.uploadAvatar(files.toTypedArray(), uuid)

            val baseUrl = getBaseUrl(request)
            val urls = arrayListOf("$baseUrl/avatar/get/$uuid")
            ResultUtil.result(200, urls, "头像上传成功")
        } catch (e: UnauthorizedException) {
            ResultUtil.result(401, e.message ?: "Authentication failed")
        } catch (e: Exception) {
            ResultUtil.result(500, "头像上传异常: ${e.message}")
        }
    }

    @PostMapping("/file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @Parameter(hidden = true)
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        @RequestPart("file") files: List<MultipartFile>,
        @Parameter(hidden = true) request: HttpServletRequest
    ): Result<out Serializable> {

        if (authorization.isNullOrBlank()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.code, "未登录")
        }

        if (files.isEmpty() || files[0].isEmpty) {
            return ResultUtil.result(400, "上传失败：请至少选择一个文件")
        }

        return try {
            val token = if (authorization.startsWith(BEARER_PREFIX)) authorization.substring(7) else authorization
            val claims = jwtUtils.parseToken(token)
            val uuid = claims.subject

            if (loginService.isKickedOut(uuid, claims.issuedAt)) {
                return ResultUtil.result(ResultEnum.UNAUTHORIZED.code, MSG_CREDENTIALS_EXPIRED)
            }

            val savedFiles = fileUploadService.uploadOrdinaryFile(files.toTypedArray(), uuid)
            val baseUrl = getBaseUrl(request)
            val urls = savedFiles.map { "$baseUrl/file/get/${it.id}" }
            ResultUtil.result(200, ArrayList(urls), "文件上传成功")
        } catch (e: UnauthorizedException) {
            ResultUtil.result(401, e.message ?: "Authentication failed")
        } catch (e: Exception) {
            ResultUtil.result(500, "文件上传异常: ${e.message}")
        }
    }

    @GetMapping("/avatar/get/{uuid}")
    fun getAvatar(
        @PathVariable uuid: String,
        @Parameter(hidden = true)
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        webRequest: WebRequest
    ): ResponseEntity<Any> {

        var targetUuid = uuid
        if ("user".equals(uuid, ignoreCase = true)) {
            if (authorization.isNullOrBlank()) {
                 return ResponseEntity.status(401).body(ResultUtil.result(ResultEnum.UNAUTHORIZED.code, "未登录"))
            }
            try {
                val token = if (authorization.startsWith(BEARER_PREFIX)) authorization.substring(7) else authorization
                val claims = jwtUtils.parseToken(token)
                targetUuid = claims.subject

                if (loginService.isKickedOut(targetUuid, claims.issuedAt)) {
                    return ResponseEntity.status(401).body(ResultUtil.result(ResultEnum.UNAUTHORIZED.code, MSG_CREDENTIALS_EXPIRED))
                }
            } catch (_: Exception) {
                 return ResponseEntity.status(401).body(ResultUtil.result(ResultEnum.UNAUTHORIZED.code, "Token无效"))
            }
        }

        val redisEtag = redisTemplate.opsForValue()["avatar:etag:$targetUuid"]
        if (redisEtag != null && webRequest.checkNotModified(redisEtag)) {
            return ResponseEntity.status(304).build()
        }

        return try {
            val resource = fileUploadService.downloadAvatarStream(targetUuid)
                ?: return ResponseEntity.status(404).body(ResultUtil.result(404, "头像不存在"))

            val uploadDate = resource.gridFSFile?.uploadDate?.time?.toString() ?: System.currentTimeMillis().toString()
            val currentEtag = redisEtag ?: uploadDate

            val mediaType = runCatching { MediaType.parseMediaType(resource.contentType) }
                .getOrDefault(MediaType.APPLICATION_OCTET_STREAM)

            ResponseEntity.ok()
                .contentType(mediaType)
                .eTag(currentEtag)
                .body(InputStreamResource(resource.inputStream))

        } catch (e: Exception) {
            ResponseEntity.status(500).body(ResultUtil.result(500, "获取头像异常: ${e.message}"))
        }
    }

    @GetMapping("/file/get/{id}")
    fun getFile(
        @PathVariable id: String,
        webRequest: WebRequest
    ): ResponseEntity<Any> {
        val redisEtag = redisTemplate.opsForValue()["file:etag:$id"]
        if (redisEtag != null && webRequest.checkNotModified(redisEtag)) {
            return ResponseEntity.status(304).build()
        }

        return try {
            val resource = fileUploadService.downloadOrdinaryFileStream(id)
                ?: return ResponseEntity.status(404).body(ResultUtil.result(404, "文件不存在"))

            val uploadDate = resource.gridFSFile?.uploadDate?.time?.toString() ?: System.currentTimeMillis().toString()
            val currentEtag = redisEtag ?: uploadDate

            val mediaType = runCatching { MediaType.parseMediaType(resource.contentType) }
                .getOrDefault(MediaType.APPLICATION_OCTET_STREAM)

            val fileName = resource.filename ?: "unknown_file"
            val disposition = "inline; filename=\"${URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")}\""

            ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .eTag(currentEtag)
                .body(InputStreamResource(resource.inputStream)) // 🔥 流式输出

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