package com.las.backenduser.service;

import com.las.backenduser.model.dto.register.RegisterCompleteDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 注册业务逻辑接口
 * @author Mu Yang
 */
public interface RegisterService {

    /**
     * 生成激活 Token
     *
     * @param qq        QQ号
     * @param direction 审核方向 0:红石 1:后勤 2:其他
     * @param expireMs  过期时间(毫秒戳)
     * @return token 字符串
     */
    String generateToken(String qq, int direction, long expireMs);

    /**
     * 校验并解析 Token
     *
     * @param token 激活链接中的 token
     * @return 如果校验成功，返回包含 qq 和 direction 的 Map；失败抛出 IllegalArgumentException
     */
    Map<String, Object> verifyAndDecodeToken(String token);

    /**
     * 校验 Minecraft ID
     *
     * @param username Minecraft 用户名
     * @return UUID 字符串，如果不存在则返回 null
     */
    String checkMinecraftId(String username) throws IOException;

    HashMap<String, Object> activateToken(String token);

    void completeRegister(RegisterCompleteDTO dto) throws IOException;
}