package com.las.backenduser.model.dto.register;

import lombok.Data;

/**
 * 生成注册链接请求参数
 * @author Mu Yang
 */
@Data
public class GenerateLinkDTO {
    /**
     * QQ号
     */
    private String qq;

    /**
     * 审核方向 0:红石 1:后勤 2:其他
     */
    private Integer direction;
}