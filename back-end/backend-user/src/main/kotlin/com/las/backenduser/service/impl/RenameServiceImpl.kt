package com.las.backenduser.service.impl

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.las.backenduser.mapper.UserMapper
import com.las.backenduser.model.User
import com.las.backenduser.service.RenameService
import com.las.backenduser.utils.result.Result
import com.las.backenduser.utils.result.ResultEnum
import com.las.backenduser.utils.result.ResultUtil
import org.springframework.stereotype.Service
import java.io.Serializable

@Service
class RenameServiceImpl(
    private val userMapper: UserMapper
) : RenameService {

    override fun rename(uuid: String, newName: String): Result<Serializable> {
        if (newName.isBlank()) {
            return ResultUtil.result(403, "用户名不能为空")
        }

        // 检查是否重名
        val queryWrapper = QueryWrapper<User>().eq("username", newName)
        val count = userMapper.selectCount(queryWrapper)
        if (count > 0) {
            return ResultUtil.result(403, "用户名已存在")
        }

        val user = userMapper.selectById(uuid)
        if (user == null) {
            return ResultUtil.result(ResultEnum.FAIL.code, "用户不存在")
        }

        user.username = newName
        userMapper.updateById(user)

        return ResultUtil.result(ResultEnum.SUCCESS.code, "用户名修改成功")
    }
}