package com.las.backenduser.model.dto.register;

import lombok.Data;

/**
 * 提交注册完整信息请求参数
 * @author Mu Yang
 */
@Data
public class RegisterCompleteDTO {
    /**
     * 激活链接中的 token
     */
    private String token;

    /**
     * Minecraft 游戏 ID
     */
    private String minecraftId;

    /**
     * 密码
     */
    private String password;

    /**用户名**/
    private String username;

}