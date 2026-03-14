package com.las.backenduser.service;

import com.las.backenduser.model.dto.user.UserPageVO;

import java.util.List;

/**
 * 用户管理业务接口
 */
public interface UserService {

    /**
     * 分页查询所有用户
     *
     * @param page   页码（从1开始）
     * @param size   每页大小
     * @param search 搜索关键词（可选，模糊匹配uuid/用户名/minecraftId）
     * @return 分页用户数据
     */
    UserPageVO getAllUsers(int page, int size, String search);

    /**
     * 更新用户权限组
     *
     * @param uuid       用户 UUID
     * @param permission 新的权限列表
     */
    void updatePermission(String uuid, List<String> permission);
}

