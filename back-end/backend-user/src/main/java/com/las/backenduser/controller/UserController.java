package com.las.backenduser.controller;

import com.las.backenduser.model.dto.user.UserPageVO;
import com.las.backenduser.service.UserService;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 分页查询所有用户
     *
     * @param page   页码，从1开始，默认1
     * @param size   每页大小，默认10
     * @param search 搜索关键词（可选，模糊匹配uuid/用户名/qq）
     * @return 分页用户数据
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
}

