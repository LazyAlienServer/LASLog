package com.las.backenduser.controller.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backenduser.controller.LoginController;
import com.las.backenduser.model.dto.login.LoginRequestDTO;
import com.las.backenduser.service.impl.LoginServiceImpl;
import com.las.backenduser.utils.cookie.CookieUtils;
import com.las.backenduser.utils.result.Result;
import com.las.backenduser.utils.result.ResultEnum;
import com.las.backenduser.utils.result.ResultUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.Serializable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginServiceImpl loginService;

    @MockitoBean
    private CookieUtils cookieUtils;

    @MockitoBean
    private com.las.backenduser.websocket.WsClientService wsClientService;

    @MockitoBean
    private com.las.backenduser.mapper.UserMapper userMapper;

    @Test
    @DisplayName("POST /login/login - 成功返回200")
    void testLoginEndpoint() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("test");
        dto.setPassword("pwd");
        dto.setClientId("pc");

        Result<String> mockResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "{\"AT\":\"at\",\"RT\":\"rt\"}", "登陆成功");
        when(loginService.login("test", "pwd", "pc")).thenReturn(mockResult);

        mockMvc.perform(post("/login/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("登陆成功"));
    }

    @Test
    @DisplayName("POST /login/kickByUuid - 成功踢人")
    void testKickEndpoint() throws Exception {
        Result<Serializable> mockResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "踢出成功");
        when(loginService.kickOutByUuid("uuid-123")).thenReturn(mockResult);

        mockMvc.perform(post("/login/kickByUuid")
                        .param("userUuid", "uuid-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("踢出成功"));
    }

    @Test
    @DisplayName("GET /login/checkAuth - 缺少AT Cookie被拦截")
    void testCheckAuth_MissingCookie() throws Exception {
        when(CookieUtils.getCookieValue(any(), eq(CookieUtils.AT_COOKIE))).thenReturn(null);

        mockMvc.perform(get("/login/checkAuth")
                        .param("clientId", "pc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("未提供 AccessToken"));
    }

    @Test
    @DisplayName("GET /login/checkAuth - 携带AT Cookie正常校验")
    void testCheckAuth_WithCookie() throws Exception {
        Result<Serializable> mockResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "登录成功");
        when(CookieUtils.getCookieValue(any(), eq(CookieUtils.AT_COOKIE))).thenReturn("valid_token");
        when(loginService.loginByToken("valid_token", "pc")).thenReturn(mockResult);

        mockMvc.perform(get("/login/checkAuth")
                        .param("clientId", "pc")
                        .cookie(new Cookie(CookieUtils.AT_COOKIE, "valid_token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }


    @Test
    @DisplayName("POST /login/kickByUserName - 成功按用户名踢人")
    void testKickByUsernameEndpoint() throws Exception {
        Result<Serializable> mockResult = ResultUtil.result(ResultEnum.SUCCESS.getCode(), "踢出成功");
        when(loginService.kickOutByUsername("testuser")).thenReturn(mockResult);

        mockMvc.perform(post("/login/kickByUserName")
                        .param("userName", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("踢出成功"));
    }
}