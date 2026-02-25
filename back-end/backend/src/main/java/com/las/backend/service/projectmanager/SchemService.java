package com.las.backend.service.projectmanager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.utils.result.Result;

public interface SchemService {

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