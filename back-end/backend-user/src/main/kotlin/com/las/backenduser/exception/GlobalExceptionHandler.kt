package com.las.backenduser.exception

import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultUtil
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.io.Serializable

@RestControllerAdvice(basePackages = ["com.las.backenduser.controller"])
class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(): Result<out Serializable> {
        return ResultUtil.result(413, "上传失败：文件大小超出了服务器允许的最大限制")
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(e: Exception): Result<out Serializable> {
        e.printStackTrace()
        return ResultUtil.result(500, "服务器内部异常: ${e.message}")
    }
}