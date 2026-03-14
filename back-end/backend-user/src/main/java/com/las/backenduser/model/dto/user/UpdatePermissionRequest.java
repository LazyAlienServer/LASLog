package com.las.backenduser.model.dto.user;

import lombok.Data;

import java.util.List;

/**
 * 修改用户权限组请求体
 */
@Data
public class UpdatePermissionRequest {

    /**
     * 新的权限列表（如 ["member", "builder"]）
     */
    private List<String> permission;
}

