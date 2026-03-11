package com.las.backenduser.service;

import com.las.backenduser.model.dto.user.UserPageVO;

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
}

