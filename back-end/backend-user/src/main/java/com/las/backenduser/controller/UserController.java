package com.las.backenduser.controller;

 import com.las.backenduser.model.dto.user.UpdatePermissionRequest;
import com.las.backenduser.model.dto.user.UserPageVO;
import com.las.backenduser.service.UserService;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 分页查询所有用户
     */
    @GetMapping("/list")
    public Result<UserPageVO> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        try {
            log.info("查询用户列表: page={}, size={}, search={}", page, size, search);
            UserPageVO data = userService.getAllUsers(page, size, search);
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), data, "查询成功");
        } catch (Exception e) {
            log.error("查询用户列表异常: {}", e.getMessage(), e);
            return ResultUtil.result(ResultEnum.FAIL.getCode(), null, "查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户权限组
     *
     * @param uuid    用户 UUID
     * @param request 包含新 permission 列表的请求体
     */
    @PutMapping("/{uuid}/permission")
    public Result<Serializable> updatePermission(
            @PathVariable String uuid,
            @RequestBody UpdatePermissionRequest request) {
        try {
            log.info("更新用户权限组: uuid={}, permission={}", uuid, request.getPermission());
            userService.updatePermission(uuid, request.getPermission());
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "更新成功");
        } catch (Exception e) {
            log.error("更新用户权限组异常: {}", e.getMessage(), e);
            return ResultUtil.result(ResultEnum.FAIL.getCode(), "更新失败：" + e.getMessage());
        }
    }
}

