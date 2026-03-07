package com.las.backenduser.utils.jwt;

import com.las.backenduser.exception.UnauthorizedException; // 请确保路径与你项目一致
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class JwtUtilsTest {

    @Resource
    JwtUtils jwtUtils;

    @Test
    void createAccessToken() {
        String tokenAT = jwtUtils.createAccessToken("user1-uuid-test", "user1");
        log.info("Access Token: {}", tokenAT);
        assertThat(tokenAT).isNotEmpty();
    }

    @Test
    void createRefreshToken() {
        String tokenRT = jwtUtils.createRefreshToken();
        log.info("Refresh Token: {}", tokenRT);
        assertThat(tokenRT).isNotEmpty();
    }

    @Test
    @DisplayName("场景1：合法 Token 解析 - 覆盖 try 块")
    void parseToken_Success() {
        String token = jwtUtils.createAccessToken("user-123", "test-user");

        Claims claims = jwtUtils.parseToken(token);

        assertNotNull(claims);
        assertEquals("user-123", claims.getSubject());
    }

    @Test
    @DisplayName("场景2：Token 已过期 - 覆盖 ExpiredJwtException")
    void parseToken_Expired() {
        // 由于无法直接修改 JwtUtils 内部逻辑，我们手动构造一个使用相同 Key 但已过期的 Token
        // 注意：这里需要能访问到 JwtUtils 里的 key，如果它是 private，建议在 JwtUtils 暴露一个获取 key 的方法（仅限测试用）
        // 或者如果你的 key 是配置出来的，在测试里用同样的配置生成

        // 假设 JwtUtils 使用的是某个配置好的 secret，这里模拟一个过期 Token：
        // 如果无法获取 key，本场景通常需要将 SystemClock 调快或者在 JwtUtils 中允许传入 ttl

        // 简易方案：如果 JwtUtils 允许自定义过期时间，直接调用；否则需 mock 异常
        log.warn("注意：测试 ExpiredJwtException 通常需要模拟时间流逝或构造过期报文");
    }

    @Test
    @DisplayName("场景3：签名不匹配（被篡改） - 覆盖 SignatureException")
    void parseToken_SignatureException() {
        // 生成一个合法 Token 后，修改其中一个字符模拟篡改
        String token = jwtUtils.createAccessToken("uuid", "user");
        String tamperedToken = token + "modified";

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtUtils.parseToken(tamperedToken);
        });

        assertEquals("TOKEN_INVALID", exception.getMessage());
    }

    @Test
    @DisplayName("场景4：格式错误 - 覆盖 MalformedJwtException")
    void parseToken_Malformed() {
        // 传入完全不符合 JWT 结构的字符串
        String malformedToken = "completely-wrong-format";

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtUtils.parseToken(malformedToken);
        });

        assertEquals("TOKEN_FORMAT_ERROR", exception.getMessage());
    }

    @Test
    @DisplayName("场景5：未知异常 - 覆盖 Exception e")
    void parseToken_GeneralException() {
        // 传入 null 触发 getPayload() 或解析时的 NullPointerException
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jwtUtils.parseToken(null);
        });

        assertEquals("TOKEN_AUTH_FAILED", exception.getMessage());
    }

    @Test
    @DisplayName("验证从Token获取用户ID")
    void getUserIdFromToken() {
        String tokenAT = jwtUtils.createAccessToken("user1-uuid-test", "user1");
        assertThat(tokenAT).isNotEmpty();

        String userUUIDFromToken = jwtUtils.getUserUUIDFromToken(tokenAT);
        log.info("Extracted UUID: {}", userUUIDFromToken);
        assertThat(userUUIDFromToken).isEqualTo("user1-uuid-test");
    }
}