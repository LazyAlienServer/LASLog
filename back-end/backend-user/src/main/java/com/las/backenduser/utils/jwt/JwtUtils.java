package com.las.backenduser.utils.jwt;

import com.las.backenduser.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtils {

    private final SecretKey key;

    //Access Token 过期时间 (30 分钟)
    private static final long AT_EXPIRATION =
            Duration.ofMinutes(30).toMillis();

    // 秘钥256位
    public JwtUtils(@Value("${secret.jwt.key}") String jwtKey) {
        this.key = Keys.hmacShaKeyFor(jwtKey.getBytes());
    }

    /**
     * 生成 Access Token (JWT)
     *
     * @param userUUId   用户 ID (存入 subject)
     * @param userName 用户名 (自定义 claim)
     * @return 塑封后的 JWT 字符串
     */
    public String createAccessToken(String userUUId, String userName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + AT_EXPIRATION);

        return Jwts.builder()
                .header().add("typ", "JWT").and() // 显式声明类型
                .subject(userUUId)
                .claim("userName", userName)      // 扩展字段
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS256)    // 显式指定算法
                .compact();
    }

    /**
     * 生成 Refresh Token
     * <p>
     * 注意：Refresh Token 采用 UUID 纯随机字符串，不包含任何业务信息，
     * 必须配合 Redis 使用，后端拥有绝对的撤销权。
     * </p>
     *
     * @return 32 位去横杠的随机字符串
     */
    public String createRefreshToken() {
        // 性能优化：使用 replace 替代 replaceAll
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 解析并验证 Access Token
     *
     * @param token 前端传来的 JWT 字符串
     * @return 包含用户信息的 Claims 对象
     * @throws UnauthorizedException 当 Token 过期、伪造或格式错误时抛出异常
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT 令牌已过期: {}", e.getMessage());
            // 特别标记：前端收到此错误应调用 refresh 接口
            throw new UnauthorizedException("TOKEN_EXPIRED");
        } catch (SignatureException e) {
            log.error("JWT 签名验证失败（密钥不匹配或内容被篡改）");
            throw new UnauthorizedException("TOKEN_INVALID");
        } catch (MalformedJwtException e) {
            log.error("JWT 格式错误");
            throw new UnauthorizedException("TOKEN_FORMAT_ERROR");
        } catch (Exception e) {
            log.error("JWT 解析发生未知异常", e);
            throw new UnauthorizedException("TOKEN_AUTH_FAILED");
        }
    }

    /**
     * 从 Token 中直接获取用户 ID
     *
     * @param token JWT 字符串
     * @return 用户 ID
     */
    public String getUserUUIDFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
