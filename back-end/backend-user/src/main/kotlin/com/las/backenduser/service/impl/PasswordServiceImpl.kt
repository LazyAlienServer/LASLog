package com.las.backenduser.service.impl

import com.las.backenduser.mapper.UserMapper
import com.las.backenduser.service.LoginService
import com.las.backenduser.service.PasswordService
import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultEnum
import com.las.backenduser.utils.result.ResultUtil
import com.las.backenduser.utils.salt.Salt
import org.springframework.stereotype.Service
import java.io.Serializable

@Service
class PasswordServiceImpl(
    private val userMapper: UserMapper,
    private val loginService: LoginService
) : PasswordService {

    override fun changePassword(uuid: String, oldPass: String, newPass: String): Result<Serializable> {
        // 1. 查找用户
        val user = userMapper.selectById(uuid) ?: return ResultUtil.result(ResultEnum.FAIL.code, "用户不存在")

        // 2. 校验旧密码
        val oldSalt = user.salt
        val oldHash = Salt.salt(oldPass, oldSalt)
        if (oldHash != user.password) {
            return ResultUtil.result(ResultEnum.FAIL.code, "旧密码错误")
        }

        // 3. 生成新密码 (生成新盐)
        val newPasswordObj = Salt.salt(newPass)
        user.password = newPasswordObj.cipherText
        user.salt = newPasswordObj.salt

        // 4. 更新数据库
        userMapper.updateById(user)

        // 5. 踢出所有设备 (全设备重新登录)
        loginService.kickOutByUuid(uuid)

        return ResultUtil.result(ResultEnum.SUCCESS.code, "密码修改成功，请重新登录")
    }
}

