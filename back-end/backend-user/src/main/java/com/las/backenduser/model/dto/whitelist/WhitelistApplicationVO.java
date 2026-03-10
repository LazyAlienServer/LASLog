package com.las.backenduser.model.dto.whitelist;

import lombok.Data;

import java.io.Serializable;

/**
 * 白名单申请列表条目 VO
 */
@Data
public class WhitelistApplicationVO implements Serializable {

    private String id;

    private String userUuid;

    private String username;

    private String server;

    private String status;

    private Long createTime;
}

