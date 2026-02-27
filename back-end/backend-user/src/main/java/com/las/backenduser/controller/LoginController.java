package com.las.backenduser.controller;


import com.las.backenduser.model.dto.login.LoginRequestDTO;
import com.las.backenduser.service.impl.LoginServiceImpl;
import com.las.backenduser.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
