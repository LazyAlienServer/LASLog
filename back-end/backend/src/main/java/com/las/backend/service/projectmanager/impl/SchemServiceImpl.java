package com.las.backend.service.projectmanager.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backend.model.projectmanager.DataMsg;
import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.service.projectmanager.SchemService;

import com.las.backend.service.projectmanager.WsServerService;
import com.las.backend.utils.result.Result;
import com.las.backend.utils.result.ResultEnum;
import com.las.backend.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemServiceImpl implements SchemService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WsServerService wsServerService;

    /**
     * 统一处理来自 Mod 的响应，拦截错误信息
     */
    private Result handleModResponse(String resultStr, String successMsg) throws Exception {
        JsonNode jsonNode = mapper.readTree(resultStr);

        // 如果 Mod 传回了 error 字段，说明发生错误，直接返回给前端
        if (jsonNode.has("error")) {
            String errorMsg = jsonNode.get("error").asText();
            log.warn("Mod返回错误: {}", errorMsg);
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), errorMsg);
        }

        // 如果没有 error，说明是正常数据，转换为 Object 后包裹进 SUCCESS
        Object data = mapper.readValue(resultStr, Object.class);
        return ResultUtil.result(ResultEnum.SUCCESS.getCode(), data, successMsg);
    }

    @Override
    public Result getSchemFiles() {
        try {
            String resultStr = wsServerService.sendAndAwait("GET_SCHEM_FILES", "{}").get();
            return handleModResponse(resultStr, "获取投影文件信息成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取投影文件信息失败: 线程被中断");
        } catch (Exception e) {
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取投影文件信息失败: " + e.getMessage());
        }
    }

    @Override
    public Result getProgress(String filename) {
        try {
            DataMsg msg = new DataMsg(filename);
            String sendJson = mapper.writeValueAsString(msg);

            String resultStr = wsServerService.sendAndAwait("GET_PROGRESS_TASK", sendJson).get();
            return handleModResponse(resultStr, "获取进度成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取进度失败: 线程被中断");
        } catch (Exception e) {
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取进度失败: " + e.getMessage());
        }
    }

    @Override
    public Result getMissingMaterial(MaterialReq req) {
        try {
            DataMsg msg = new DataMsg(
                    req.getFilename(),
                    req.getMx1(), req.getMy1(), req.getMz1(),
                    req.getMx2(), req.getMy2(), req.getMz2(),
                    req.isIncludeBuilt()
            );
            String sendJson = mapper.writeValueAsString(msg);

            String resultStr = wsServerService.sendAndAwait("GET_MATERIAL_TASK", sendJson).get();
            return handleModResponse(resultStr, "获取缺失材料成功");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取缺失材料失败: 线程被中断");
        } catch (Exception e) {
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取缺失材料失败: " + e.getMessage());
        }
    }
}