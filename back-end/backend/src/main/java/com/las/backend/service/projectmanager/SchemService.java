package com.las.backend.service.projectmanager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.utils.result.Result;

import java.util.concurrent.ExecutionException;

public interface SchemService {

    /**
     * 获取投影文件列表及文件信息
     * @return 标准返回
     */
    Result getSchemFiles() throws ExecutionException, InterruptedException;

    /**
     * 获取进度
     * @param filename 投影文件名
     * @return 标准返回
     */
    Result getProgress(String filename);

    /**
     * 获取缺失材料
     * @param req MaterialReq - 指定材料区域
     * @return 标准返回
     */
    Result getMissingMaterial(MaterialReq req);
}