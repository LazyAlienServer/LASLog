package com.las.backend.service.projectManager;

import com.las.backend.model.projectManager.MaterialReq;
import com.las.backend.utils.result.Result;

public interface SchemService {

    // 获取进度
    Result getProgress(String filename);

    // 获取缺失材料
    Result getMissingMaterial(MaterialReq req);
}