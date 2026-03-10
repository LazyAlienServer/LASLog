package com.las.backenduser.model.dto.whitelist;

import lombok.Data;

import java.io.Serializable;

/**
 * 白名单操作请求 DTO（管理员用）
 */
@Data
public class WhitelistActionDTO implements Serializable {

    /** 目标玩家的 Minecraft UUID */
    private String minecraftUuid;

    /** 服务器名称 */
    private String server;

    /**
     * 封禁时长（天），仅 action=ban 时有效
     * 0 或 null 表示永久封禁
     */
    private Integer banDays;
}

