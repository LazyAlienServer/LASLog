package com.las.backenduser.controller;


import com.las.backenduser.model.dto.login.LoginRequestDTO;
import com.las.backenduser.service.impl.LoginServiceImpl;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

@RestController
@RequestMapping("/login")
@Slf4j
public class LoginController {

    private final LoginServiceImpl loginService;

    public LoginController(LoginServiceImpl loginService) {
        this.loginService = loginService;
    }


    @PostMapping("/login")
    public Result<String> login (@RequestBody LoginRequestDTO loginRequestDTO){
        return loginService.login(loginRequestDTO.getUsername(),loginRequestDTO.getPassword(), loginRequestDTO.getClientId());
    }

    @PostMapping("/kickByUuid")
    public Result<Serializable> kick(@RequestParam String userUuid){
       return loginService.kickOutByUuid(userUuid);
    }

    @PostMapping("/kickByUserName")
    public Result<Serializable> kickByUserName(@RequestParam String userName){
        return loginService.kickOutByUsername(userName);
    }

    @PostMapping("/logout")
    public Result<Serializable> logout(@RequestParam String userUuid, @RequestParam String clientId){
        return loginService.logout(userUuid, clientId);
    }

    @GetMapping("/loginByToken")
    public Result<Serializable> loginByToken(
            @RequestParam String accessToken,
            @RequestParam String clientId) {
        if (accessToken == null || accessToken.trim().isEmpty()){
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(),"未提供 AccessToken");
        }
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        return loginService.loginByToken(accessToken, clientId);
    }

    @PostMapping("refreshToken")
    public Result<Serializable> refreshToken(@RequestParam String refreshToken, @RequestParam String clientId){
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResultUtil.result(ResultEnum.UNAUTHORIZED.getCode(),"未提供 RefreshToken");
        }
        return loginService.refreshToken(refreshToken, clientId);
    }
}
