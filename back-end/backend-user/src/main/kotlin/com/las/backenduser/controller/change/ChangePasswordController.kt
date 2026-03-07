package com.las.backenduser.controller.change

import com.las.backenduser.exception.UnauthorizedException
import com.las.backenduser.service.LoginService
import com.las.backenduser.service.PasswordService
import com.las.backenduser.utils.jwt.JwtUtils
import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultUtil
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.*
import java.io.Serializable

@RestController
@RequestMapping("/change")
class ChangePasswordController(
    private val passwordService: PasswordService,
    private val jwtUtils: JwtUtils,
    private val loginService: LoginService
) {

    @PostMapping("/password")
    fun changePassword(
        @Parameter(hidden = true)
        @RequestHeader(value = "Authorization", required = false) authorization: String?,
        @RequestParam oldPass: String,
        @RequestParam newPass: String
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

            passwordService.changePassword(uuid, oldPass, newPass)
        } catch (e: UnauthorizedException) {
            ResultUtil.result(401, e.message ?: "Authentication failed")
        } catch (e: Exception) {
            e.printStackTrace()
            ResultUtil.result(500, "操作失败: ${e.message}")
        }
    }
}
