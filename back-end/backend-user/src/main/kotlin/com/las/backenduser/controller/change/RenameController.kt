package com.las.backenduser.controller.change

import com.las.backenduser.exception.UnauthorizedException
import com.las.backenduser.service.LoginService
import com.las.backenduser.service.RenameService
import com.las.backenduser.utils.jwt.JwtUtils
import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultUtil
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

@RestController
@RequestMapping("/change")
class RenameController(
    private val renameService: RenameService,
    private val jwtUtils: JwtUtils,
    private val loginService: LoginService
) {

    @PostMapping("/rename")
    fun rename(
        @Parameter(hidden = true)
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        @RequestParam newName: String
    ): Result<Serializable> {
        if (authorization.isNullOrBlank()) {
            return ResultUtil.result(403, "未登录")
        }

        val token = if (authorization.startsWith("Bearer ")) {
            authorization.substring(7)
        } else {
            authorization
        }

        return try {
            val claims = jwtUtils.parseToken(token)
            val uuid = claims.subject

            if (loginService.isKickedOut(uuid, claims.issuedAt)) {
                return ResultUtil.result(401, "凭证已失效，请重新登录")
            }

            renameService.rename(uuid, newName)
        } catch (e: UnauthorizedException) {
            ResultUtil.result(401, e.message ?: "Authentication failed")
        } catch (e: Exception) {
            ResultUtil.result(403, "操作失败: ${e.message}")
        }
    }
}