package com.las.backenduser.controller;

import com.las.backenduser.model.dto.register.GenerateLinkDTO;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;
import com.las.backenduser.service.RegisterService;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
@Slf4j
public class RegisterController {

    private final RegisterService registerService;

    @PostMapping("/generateLink")
    public Result<String> generateLink(@RequestBody GenerateLinkDTO dto) {
        if (dto.getQq() == null || dto.getDirection() == null) {
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), null, "QQ号和审核方向不能为空");
        }

        long expireTime = System.currentTimeMillis() + 20 * 60 * 1000L;
        String token = registerService.generateToken(dto.getQq(), dto.getDirection(), expireTime);
        String activationUrl = "https://domain.com/activate?token=" + token;

        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), activationUrl, "链接生成成功");
    }

    @GetMapping("/check-mc-id")
    public Result<String> checkMinecraftId(@RequestParam String username) throws IOException {
        if (username == null || username.trim().isEmpty()) {
            // 强转 null 解决泛型推导问题
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), (String) null, "Minecraft ID 不能为空");
        }

        String uuid = registerService.checkMinecraftId(username);
        if (uuid != null) {
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), uuid, "Minecraft ID 校验通过");
        } else {
            // 强转 null 解决泛型推导问题
            return ResultUtil.result(ResultEnum.NOT_FOUND.getCode(), (String) null, "Minecraft ID 不存在");
        }
    }

    @GetMapping("/activate")
    public Result<HashMap<String, Object>> activateToken(@RequestParam String token) {
        try {
            // 调用 Service 获取清洗后的数据
            HashMap<String, Object> returnData = registerService.activateToken(token);
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), returnData, "Token有效，请填写注册信息");
        } catch (IllegalArgumentException e) {
            // 强转 null 为 HashMap，满足 T extends Serializable 且匹配方法返回值
            return ResultUtil.result(403, null, e.getMessage());
        }
    }

    @PostMapping("/complete")
    public Result<Serializable> completeRegister(@RequestBody RegisterCompleteDTO dto) {
        try {
            registerService.completeRegister(dto);
            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), "账号注册成功！");
        } catch (IllegalArgumentException e) {
            return ResultUtil.result(403, e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResultUtil.result(500, "服务器内部错误，请稍后再试");
        }
    }
}