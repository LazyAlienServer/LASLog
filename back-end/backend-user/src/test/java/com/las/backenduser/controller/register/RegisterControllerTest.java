package com.las.backenduser.controller.register;

import com.las.backenduser.controller.RegisterController;
import com.las.backenduser.model.dto.register.GenerateLinkDTO;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;
import com.las.backenduser.service.RegisterService;
import com.las.backenduser.utils.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private RegisterService registerService;

    @InjectMocks
    private RegisterController registerController;

    private GenerateLinkDTO generateLinkDTO;
    private RegisterCompleteDTO completeDTO;

    @BeforeEach
    void setUp() {
        generateLinkDTO = new GenerateLinkDTO();
        generateLinkDTO.setQq("123456789");
        generateLinkDTO.setDirection(0);

        completeDTO = new RegisterCompleteDTO();
        completeDTO.setToken("valid.token");
        completeDTO.setMinecraftId("Notch");
        completeDTO.setPassword("password123");
        completeDTO.setUsername("Notch");
    }

    // --- generateLink Tests ---
    @Test
    void generateLink_Success() {
        when(registerService.generateToken(anyString(), anyInt(), anyLong())).thenReturn("mocked_token.sign");
        Result<String> result = registerController.generateLink(generateLinkDTO);
        assertEquals(200, result.getCode());
        assertTrue(result.getData().contains("mocked_token.sign"));
    }

    @Test
    void generateLink_MissingParams() {
        generateLinkDTO.setQq(null);
        Result<String> result = registerController.generateLink(generateLinkDTO);
        assertEquals(403, result.getCode()); // SERVER_ERROR is 403 in your Enum

        generateLinkDTO.setQq("123");
        generateLinkDTO.setDirection(null);
        Result<String> result2 = registerController.generateLink(generateLinkDTO);
        assertEquals(403, result2.getCode());
    }

    // --- checkMinecraftId Tests ---
    @Test
    void checkMinecraftId_Success() throws IOException {
        when(registerService.checkMinecraftId("Notch")).thenReturn("mocked-uuid");
        Result<String> result = registerController.checkMinecraftId("Notch");
        assertEquals(200, result.getCode());
        assertEquals("mocked-uuid", result.getData());
    }

    @Test
    void checkMinecraftId_BlankUsername() throws IOException {
        Result<String> result = registerController.checkMinecraftId("");
        assertEquals(403, result.getCode());

        Result<String> resultNull = registerController.checkMinecraftId(null);
        assertEquals(403, resultNull.getCode());
    }

    @Test
    void checkMinecraftId_NotFound() throws IOException {
        when(registerService.checkMinecraftId("UnknownUser")).thenReturn(null);
        Result<String> result = registerController.checkMinecraftId("UnknownUser");
        assertEquals(404, result.getCode());
    }

    // --- activateToken Tests ---
    @Test
    void activateToken_Success() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("qq", "123456789");
        when(registerService.activateToken("valid")).thenReturn(map);

        Result<HashMap<String, Object>> result = registerController.activateToken("valid");
        assertEquals(200, result.getCode());
        assertEquals("123456789", result.getData().get("qq"));
    }

    @Test
    void activateToken_Exception() {
        when(registerService.activateToken("invalid")).thenThrow(new IllegalArgumentException("Token非法"));
        Result<HashMap<String, Object>> result = registerController.activateToken("invalid");
        assertEquals(403, result.getCode());
        assertNull(result.getData());
        assertEquals("Token非法", result.getMsg());
    }

    // --- completeRegister Tests ---
    @Test
    void completeRegister_Success() throws IOException {
        doNothing().when(registerService).completeRegister(any(RegisterCompleteDTO.class));
        Result<Serializable> result = registerController.completeRegister(completeDTO);
        assertEquals(200, result.getCode());
    }

    @Test
    void completeRegister_IllegalArgumentException() throws IOException {
        doThrow(new IllegalArgumentException("已被使用")).when(registerService).completeRegister(any());
        Result<Serializable> result = registerController.completeRegister(completeDTO);
        assertEquals(403, result.getCode());
        assertEquals("已被使用", result.getMsg());
    }

    @Test
    void completeRegister_GenericException() throws IOException {
        doThrow(new RuntimeException("数据库炸了")).when(registerService).completeRegister(any());
        Result<Serializable> result = registerController.completeRegister(completeDTO);
        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误，请稍后再试", result.getMsg());
    }
}