package com.las.backend.service.projectmanager.impl;

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


    // 测试 4: 获取投影文件列表的测试
    @Test
    void getSchemFiles_Success() throws Exception {
        String mockModResponse = "[{\"file_name\":\"Test1\"}, {\"file_name\":\"Test2\"}]";

        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.eq("GET_SCHEM_FILES"), Mockito.eq("{}")))
                .thenReturn(CompletableFuture.completedFuture(mockModResponse));

        Result result = schemService.getSchemFiles();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData(), "解析后的数据不应该为空");
        assertEquals("获取投影文件信息成功", result.getMsg());
    }

    @Test
    void getSchemFiles_GeneralException() throws Exception {
        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new RuntimeException("模拟的网络发送异常"));

        Result result = schemService.getSchemFiles();

        assertEquals(403, result.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(result.getMsg().contains("获取投影文件信息失败"));
    }

    @Test
    void getSchemFiles_InterruptedException() throws Exception {
        CompletableFuture mockFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(mockFuture.get()).thenThrow(new InterruptedException("模拟的线程打断异常"));

        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.eq("GET_SCHEM_FILES"), Mockito.eq("{}")))
                .thenReturn(  mockFuture);

        Result result = schemService.getSchemFiles();

        assertEquals(403, result.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted(), "线程应该被重新标记为中断状态");

        Thread.interrupted();
    }

    @Test
    void getMissingMaterial_InterruptedException() throws Exception {
        // 1. 准备请求参数
        MaterialReq req = new MaterialReq();
        req.setFilename("TestBuilding");
        req.setMx1(0); req.setMy1(0); req.setMz1(0);
        req.setMx2(10); req.setMy2(10); req.setMz2(10);
        req.setIncludeBuilt(true);

        CompletableFuture mockFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(mockFuture.get()).thenThrow(new InterruptedException("模拟的材料查询线程打断异常"));

        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.eq("GET_MATERIAL_TASK"), Mockito.anyString()))
                .thenReturn(mockFuture);

        Result result = schemService.getMissingMaterial(req);

        assertEquals(403, result.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(result.getMsg().contains("获取缺失材料失败"));

        org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted(), "线程没有被正确标记为中断状态！");

        Thread.interrupted();
    }
}