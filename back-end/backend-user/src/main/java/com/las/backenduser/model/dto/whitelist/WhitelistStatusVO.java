package com.las.backenduser.model.dto.whitelist;

import lombok.Data;

import java.io.Serializable;

/**
 * 白名单状态查询结果 VO
 */
@Data
public class WhitelistStatusVO implements Serializable {

    /**
     * 白名单状态：1=有白名单, 0=无白名单, -1=封禁
     */
    private Integer status;

    /**
     * 封禁到期时间戳（毫秒，UTC）
     * 仅 status=-1 时有意义。
     * -1 表示永久封禁（≥100年后过期）
     */
    private Long banExpireAt;
}

