package com.las.backend.service.projectmanager.impl;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.WsServerService;
import com.las.backend.utils.result.Result;
import com.las.backend.utils.result.ResultEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SchemServiceImplTest {

    @Mock
    private WsServerService wsServerService;

    private SchemServiceImpl schemService;

    @BeforeEach
    void setUp() {
        schemService = new SchemServiceImpl(wsServerService);
    }

    // ================== getProgress 测试组 ==================

    @Test
    void getProgress_Success() throws Exception {
        String mockModResponse = "{\"correct\": 150, \"total\": 300}";

        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockModResponse));

        Result result = schemService.getProgress("TestBuilding");

        assertEquals(200, result.getCode(), "预期是200，但实际进入了catch块。内部报错原因为: " + result.getMsg());
        assertNotNull(result.getData());
    }

    @Test
    void getProgress_Exception() throws IOException {
        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenThrow(new RuntimeException("WebSocket 连接超时"));

        Result result = schemService.getProgress("TestBuilding");

        assertEquals(403, result.getCode());
        assertTrue(result.getMsg().contains("获取进度失败: WebSocket 连接超时"));
    }

    // 新增：测试 getProgress 的 InterruptedException 分支
    @Test
    void getProgress_InterruptedException() throws Exception {
        CompletableFuture<String> mockFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(mockFuture.get()).thenThrow(new InterruptedException("模拟的进度查询线程打断异常"));

        Mockito.lenient().when(wsServerService.sendAndAwait(eq("GET_PROGRESS_TASK"), any()))
                .thenReturn(mockFuture);

        Result result = schemService.getProgress("TestBuilding");

        assertEquals(403, result.getCode());
        assertTrue(result.getMsg().contains("获取进度失败: 线程被中断"));
        assertTrue(Thread.currentThread().isInterrupted(), "线程没有被正确标记为中断状态！");

        // 恢复线程状态，防止影响后续的测试用例
        Thread.interrupted();
    }

    // ================== getMissingMaterial 测试组 ==================

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

    @Test
    void getMissingMaterial_InterruptedException() throws Exception {
        MaterialReq req = new MaterialReq();
        req.setFilename("TestBuilding");

        CompletableFuture<String> mockFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(mockFuture.get()).thenThrow(new InterruptedException("模拟的材料查询线程打断异常"));

        Mockito.lenient().when(wsServerService.sendAndAwait(eq("GET_MATERIAL_TASK"), any()))
                .thenReturn(mockFuture);

        Result result = schemService.getMissingMaterial(req);

        assertEquals(403, result.getCode());
        assertTrue(result.getMsg().contains("获取缺失材料失败: 线程被中断"));
        assertTrue(Thread.currentThread().isInterrupted(), "线程没有被正确标记为中断状态！");

        Thread.interrupted();
    }

    // 新增：测试 getMissingMaterial 的普通 Exception 分支
    @Test
    void getMissingMaterial_GeneralException() throws IOException {
        MaterialReq req = new MaterialReq();
        req.setFilename("TestBuilding");

        // 直接在发送阶段抛出 RuntimeException
        Mockito.lenient().when(wsServerService.sendAndAwait(eq("GET_MATERIAL_TASK"), any()))
                .thenThrow(new RuntimeException("模拟的数据封装或发送异常"));

        Result result = schemService.getMissingMaterial(req);

        assertEquals(403, result.getCode());
        assertTrue(result.getMsg().contains("获取缺失材料失败: 模拟的数据封装或发送异常"));
    }

    // ================== getSchemFiles 测试组 ==================

    @Test
    void getSchemFiles_Success() throws Exception {
        String mockModResponse = "[{\"file_name\":\"Test1\"}, {\"file_name\":\"Test2\"}]";

        Mockito.lenient().when(wsServerService.sendAndAwait(eq("GET_SCHEM_FILES"), eq("{}")))
                .thenReturn(CompletableFuture.completedFuture(mockModResponse));

        Result result = schemService.getSchemFiles();

        assertEquals(200, result.getCode());
        assertNotNull(result.getData(), "解析后的数据不应该为空");
        assertEquals("获取投影文件信息成功", result.getMsg());
    }

    // ================== 测试 Mod 返回 error 字段的场景 ==================

    @Test
    void handleModResponse_WithErrorField() throws Exception {
        // 模拟 Mod 端由于某种业务原因（例如找不到文件），返回了一段带有 "error" 字段的 JSON
        String mockErrorResponse = "{\"error\": \"无法解析投影文件，格式损坏\"}";

        // 我们借用 getProgress 方法来触发这个内部逻辑
        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockErrorResponse));

        Result result = schemService.getProgress("BadBuilding");

        assertEquals(ResultEnum.SERVER_ERROR.getCode(), result.getCode());
        assertEquals("无法解析投影文件，格式损坏", result.getMsg());
    }

    @Test
    void getSchemFiles_GeneralException() throws IOException {
        Mockito.lenient().when(wsServerService.sendAndAwait(any(), any()))
                .thenThrow(new RuntimeException("模拟的网络发送异常"));

        Result result = schemService.getSchemFiles();

        assertEquals(403, result.getCode());
        assertTrue(result.getMsg().contains("获取投影文件信息失败"));
    }

    @Test
    void getSchemFiles_InterruptedException() throws Exception {
        CompletableFuture<String> mockFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(mockFuture.get()).thenThrow(new InterruptedException("模拟的线程打断异常"));

        Mockito.lenient().when(wsServerService.sendAndAwait(eq("GET_SCHEM_FILES"), eq("{}")))
                .thenReturn(mockFuture);

        Result result = schemService.getSchemFiles();

        assertEquals(403, result.getCode());
        assertTrue(result.getMsg().contains("获取投影文件信息失败: 线程被中断"));
        assertTrue(Thread.currentThread().isInterrupted(), "线程应该被重新标记为中断状态");

        Thread.interrupted();
    }
}