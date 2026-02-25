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


    //获取投影文件列表的测试
    @Test
    void getSchemFiles_Success() throws Exception {
        // 1. 模拟 Mod 返回的有效 JSON 字符串（比如一个包含投影信息的数组）
        String mockModResponse = "[{\"file_name\":\"Test1\"}, {\"file_name\":\"Test2\"}]";

        // 2. 告诉 Mockito：当 action 是 GET_SCHEM_FILES，且 data 是 null 时，返回上面的 JSON
        // 注意这里用的是 Mockito.isNull()，因为你的业务代码里传的是 null
        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.eq("GET_SCHEM_FILES"), Mockito.isNull()))
                .thenReturn(CompletableFuture.completedFuture(mockModResponse));

        // 3. 调用被测方法
        Result result = schemService.getSchemFiles();

        // 4. 断言验证
        assertEquals(200, result.getCode());
        assertNotNull(result.getData(), "解析后的数据不应该为空");
        assertEquals("获取投影文件信息成功", result.getMsg());
    }

    @Test
    void getSchemFiles_GeneralException() throws Exception {
        // 1. 模拟直接抛出一个 RuntimeException（比如网络发送失败）
        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.anyString(), Mockito.isNull()))
                .thenThrow(new RuntimeException("模拟的网络发送异常"));

        // 2. 调用被测方法
        Result result = schemService.getSchemFiles();

        // 3. 验证是否正确被 catch 捕获并返回了 403
        assertEquals(403, result.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(result.getMsg().contains("获取投影文件信息失败"));
    }

    @Test
    void getSchemFiles_InterruptedException() throws Exception {
        // 1. 特殊场景：我们需要模拟 .get() 方法被意外打断，抛出 InterruptedException
        // 这里我们需要显式 mock 一个 CompletableFuture 对象
        CompletableFuture<String> mockFuture = Mockito.mock(CompletableFuture.class);
        Mockito.when(mockFuture.get()).thenThrow(new InterruptedException("模拟的线程打断异常"));

        Mockito.lenient().when(wsServerService.sendAndAwait(Mockito.eq("GET_SCHEM_FILES"), Mockito.isNull()))
                .thenReturn(mockFuture);

        // 2. 调用被测方法
        Result result = schemService.getSchemFiles();

        // 3. 验证是否返回了错误码
        assertEquals(403, result.getCode());

        // 4. 【高阶验证】：验证你的 Thread.currentThread().interrupt() 是否真的执行了！
        org.junit.jupiter.api.Assertions.assertTrue(Thread.currentThread().isInterrupted(), "线程应该被重新标记为中断状态");

        // 测试完毕后，清除当前线程的中断标记，防止影响后续的其他测试运行
        Thread.interrupted();
    }
}