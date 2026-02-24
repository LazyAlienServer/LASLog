package com.las.backend.service.projectmanager;

import com.las.backend.model.projectmanager.MaterialReq;
import com.las.backend.utils.result.Result;

public interface SchemService {

    // 获取进度
    Result getProgress(String filename);

    // 获取缺失材料
    Result getMissingMaterial(MaterialReq req);
}