package com.las.backenduser.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息视图对象（脱敏，不含密码/盐值）
 */
@Data
public class UserVO implements Serializable {

    private String uuid;

    private String username;

    private Long qq;

    private List<String> minecraftIds;

    private List<String> minecraftUuids;

    private String mainMinecraftUuid;

    private Long registerDate;

    /**
     * 激活状态：0-待激活,1-已激活,2-封禁
     */
    private Integer status;

    private List<String> permission;

    private List<String> whitelist;
}

