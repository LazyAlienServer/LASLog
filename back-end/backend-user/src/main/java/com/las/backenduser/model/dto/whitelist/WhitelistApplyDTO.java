package com.las.backenduser.model.dto.whitelist;

import lombok.Data;

import java.io.Serializable;

/**
 * 白名单申请 DTO（玩家用，通过 Access Token 识别身份）
 */
@Data
public class WhitelistApplyDTO implements Serializable {

    /** 申请的服务器名称 */
    private String server;
}

