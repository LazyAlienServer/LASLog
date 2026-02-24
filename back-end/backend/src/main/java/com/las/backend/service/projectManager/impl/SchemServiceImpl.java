package com.las.backend.service.projectManager.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.las.backend.model.projectManager.DataMsg;
import com.las.backend.model.projectManager.MaterialReq;
import com.las.backend.service.projectManager.SchemService;

import com.las.backend.utils.result.Result;
import com.las.backend.utils.result.ResultEnum;
import com.las.backend.utils.result.ResultUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchemServiceImpl implements SchemService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final WsServerServiceImpl wsServerService;

    @Override
    public Result getProgress(String filename) {
        try {
            DataMsg msg = new DataMsg();
            msg.filename = filename;
            String sendJson = mapper.writeValueAsString(msg);

            String resultStr = wsServerService.sendAndAwait("GET_PROGRESS_TASK", sendJson).get();
            Object data = mapper.readValue(resultStr, Object.class);

            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), data, "获取进度成功");
        } catch (Exception e) {
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取进度失败: " + e.getMessage());
        }
    }

    @Override
    public Result getMissingMaterial(MaterialReq req) {
        try {
            DataMsg msg = new DataMsg();
            msg.filename = req.getFilename();
            msg.mx1 = req.getMx1(); msg.my1 = req.getMy1(); msg.mz1 = req.getMz1();
            msg.mx2 = req.getMx2(); msg.my2 = req.getMy2(); msg.mz2 = req.getMz2();
            msg.includeBuilt = req.isIncludeBuilt();
            String sendJson = mapper.writeValueAsString(msg);

            String resultStr = wsServerService.sendAndAwait("GET_MATERIAL_TASK", sendJson).get();
            Object data = mapper.readValue(resultStr, Object.class);

            return ResultUtil.result(ResultEnum.SUCCESS.getCode(), data, "获取缺失材料成功");
        } catch (Exception e) {
            return ResultUtil.result(ResultEnum.SERVER_ERROR.getCode(), "获取缺失材料失败: " + e.getMessage());
        }
    }
}