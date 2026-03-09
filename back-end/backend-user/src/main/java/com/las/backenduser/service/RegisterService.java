package com.las.backenduser.service;

import com.las.backenduser.model.dto.register.RecentRegistrationVO;
import com.las.backenduser.model.dto.register.RegisterCompleteDTO;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注册业务逻辑接口
 * @author Mu Yang
 */
public interface RegisterService {

    // ...existing methods...
    String generateToken(String qq, int direction, long expireMs);
    Map<String, Object> verifyAndDecodeToken(String token);
    String checkMinecraftId(String username) throws IOException;
    HashMap<String, Object> activateToken(String token);
    void completeRegister(RegisterCompleteDTO dto) throws IOException;

    /**
     * 获取最近注册条目列表（24小时内的所有状态）
     */
    List<RecentRegistrationVO> getRecentRegistrations();

    /**
     * 手动失效一个激活链接
     * @param signature 链接签名（唯一标识）
     */
    void invalidateLink(String signature);
}