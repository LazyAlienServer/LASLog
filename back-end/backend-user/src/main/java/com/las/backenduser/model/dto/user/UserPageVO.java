package com.las.backenduser.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询用户的响应体
 */
@Data
public class UserPageVO implements Serializable {

    /**
     * 当前页用户列表
     */
    private List<UserVO> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long current;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 总页数
     */
    private Long pages;
}

