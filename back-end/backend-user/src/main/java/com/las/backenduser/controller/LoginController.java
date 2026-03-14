package com.las.backenduser.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.las.backenduser.model.dto.login.LoginRequestDTO;
import com.las.backenduser.service.impl.LoginServiceImpl;
import com.las.backenduser.utils.cookie.CookieUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Objects;

@RestController
@RequestMapping("/login")
@Slf4j
public class LoginController {

    private final LoginServiceImpl loginService;
    private final CookieUtils cookieUtils;

    public LoginController(LoginServiceImpl loginService, CookieUtils cookieUtils) {
        this.loginService = loginService;
        this.cookieUtils = cookieUtils;
    }

    @PostMapping("/login")
    public Result<Serializable> login(
            @RequestBody LoginRequestDTO loginRequestDTO,
            HttpServletResponse response) {

        Result<String> serviceResult = loginService.login(
                loginRequestDTO.getUsername(),
                loginRequestDTO.getPassword(),
                loginRequestDTO.getClientId());

        if (Objects.equals(serviceResult.getCode(), ResultEnum.SUCCESS.getCode()) && serviceResult.getData() != null) {
            JSONObject tokens = JSON.parseObject(serviceResult.getData());
            String at = tokens.getString("AT");
            String rt = tokens.getString("RT");
            // AT 和 RT 均存入 HttpOnly Cookie
            cookieUtils.setAccessTokenCookie(response, at);
            cookieUtils.setRefreshTokenCookie(response, rt);
            return ResultUtil.result(serviceResult.getCode(), serviceResult.getMsg());
        }

        return ResultUtil.result(serviceResult.getCode(), serviceResult.getMsg());
    }

    @PostMapping("/kickByUuid")
    public Result<Serializable> kick(@RequestParam String userUuid) {
        return loginService.kickOutByUuid(userUuid);
    }

    @PostMapping("/kickByUserName")
    public Result<Serializable> kickByUserName(@RequestParam String userName) {
        return loginService.kickOutByUsername(userName);
    }

    @PostMapping("/logout")
    public Result<Serializable> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam String clientId) {

        String accessToken = CookieUtils.getCookieValue(request, CookieUtils.AT_COOKIE);

        if (accessToken == null || accessToken.trim().isEmpty()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "未登录，无需登出");
        }

        Result<Serializable> result = loginService.logoutByToken(accessToken, clientId);
        // 无论结果如何都清除所有 Token Cookie
        cookieUtils.clearAllTokenCookies(response);
        return result;
    }

    /**
     * 校验当前 AT Cookie 是否有效（前端路由守卫 / 页面刷新时调用）
     */
    @GetMapping("/checkAuth")
    public Result<Serializable> checkAuth(
            HttpServletRequest request,
            @RequestParam String clientId) {

        String accessToken = CookieUtils.getCookieValue(request, CookieUtils.AT_COOKIE);

        if (accessToken == null || accessToken.trim().isEmpty()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "未提供 AccessToken");
        }

        return loginService.loginByToken(accessToken, clientId);
    }

    @PostMapping("/refreshToken")
    public Result<Serializable> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam String clientId) {

        String refreshToken = CookieUtils.getCookieValue(request, CookieUtils.RT_COOKIE);

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(), "未提供 RefreshToken");
        }

        Result<Serializable> result = loginService.refreshToken(refreshToken, clientId);

        if (Objects.equals(result.getCode(), ResultEnum.SUCCESS.getCode()) && result.getData() != null) {
            // 刷新成功，将新 AT 写入 Cookie
            String newAt = result.getData().toString();
            cookieUtils.setAccessTokenCookie(response, newAt);
            return ResultUtil.result(result.getCode(), result.getMsg());
        }

        return result;
    }
}
