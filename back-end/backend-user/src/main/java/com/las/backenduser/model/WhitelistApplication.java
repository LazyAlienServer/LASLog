package com.las.backenduser.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 白名单申请记录（存 MongoDB）
 */
@Data
@Document(collection = "whitelist_application")
public class WhitelistApplication {

    @Id
    private String id;

    /** 申请者的系统 UUID */
    private String userUuid;

    /** 申请者的用户名 */
    private String username;

    /** 申请的服务器名称 */
    private String server;

    /** 申请状态: PENDING / APPROVED / REJECTED */
    private String status;

    /** 申请时间（毫秒时间戳） */
    private Long createTime;
}

