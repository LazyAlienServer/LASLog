package com.las.backenduser.model.dto.register;

import lombok.Data;

import java.io.Serializable;

/**
 * 最近注册条目视图对象
 */
@Data
public class RecentRegistrationVO implements Serializable {

    /** QQ号 */
    private String qq;

    /** 审核方向 0:红石 1:后勤 2:其他 */
    private Integer direction;

    /** Minecraft ID（注册完成后才有值） */
    private String minecraftId;

    /** 状态: WAITING / ACTIVATED / INVALIDATED */
    private String status;

    /** 链接生成时间(毫秒戳) */
    private Long createTime;

    /** 链接过期时间(毫秒戳) */
    private Long expireTime;

    /** 签名（用作条目唯一标识，前端失效链接时传此值） */
    private String signature;
}

