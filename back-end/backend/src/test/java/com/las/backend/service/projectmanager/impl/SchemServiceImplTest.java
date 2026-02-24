package com.las.backend.service.projectmanager.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.WsServerService;
import com.las.backend.utils.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SchemServiceImplTest {

    @Mock
    private WsServerService wsServerService;

    private SchemServiceImpl schemService;

    @BeforeEach
    void setUp() {
        schemService = new SchemServiceImpl(wsServerService);
    }

    // 测试 1：成功获取进度的场景
    @Test
    void getProgress_Success() throws Exception {
        String mockModResponse = "{\"correct\": 150, \"total\": 300}";

        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockModResponse));

        Result result = schemService.getProgress("TestBuilding");

        assertEquals(200, result.getCode(), "预期是200，但实际进入了catch块。内部报错原因为: " + result.getMsg());
        assertNotNull(result.getData());
    }

    // 测试 2：网络超时或抛出异常的场景
    @Test
    void getProgress_Exception() throws Exception {
        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenThrow(new RuntimeException("WebSocket 连接超时"));

        Result result = schemService.getProgress("TestBuilding");

        assertEquals(403, result.getCode());
    }

    // 测试 3：成功获取材料缺口的场景
    @Test
    void getMissingMaterial_Success() throws Exception {
        MaterialReq req = new MaterialReq();
        req.setFilename("TestBuilding");
        req.setMx1(0); req.setMy1(0); req.setMz1(0);
        req.setMx2(10); req.setMy2(10); req.setMz2(10);
        req.setIncludeBuilt(true);

        String mockModResponse = "{\"minecraft:iron_block\": 5}";

        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockModResponse));

        Result result = schemService.getMissingMaterial(req);

        assertEquals(200, result.getCode(), "预期是200，但实际进入了catch块。内部报错原因为: " + result.getMsg());
    }
}