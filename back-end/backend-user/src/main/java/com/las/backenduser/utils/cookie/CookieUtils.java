package com.las.backenduser.utils.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtils {

    private final boolean secureCookie;

    public CookieUtils(@Value("${app.cookie.secure:true}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public static final String RT_COOKIE = "refresh_token";
    public static final String AT_COOKIE = "access_token";

    private static final int RT_MAX_AGE = 14 * 24 * 60 * 60;   // 14 天（秒）
    private static final int AT_MAX_AGE = 15 * 60;              // 15 分钟（秒）

    /** Cookie 作用路径：使用 "/" 以确保所有路径（包含代理前缀 /api）都能正确携带 */
    private static final String COOKIE_PATH = "/";

    // ─── Access Token Cookie ─────────────────────────────────────────────────

    /**
     * 将 Access Token 写入 HttpOnly Cookie
     */
    public void setAccessTokenCookie(@NonNull HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(AT_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(AT_MAX_AGE);
        response.addCookie(cookie);
    }

    /**
     * 清除 Access Token Cookie
     */
    public void clearAccessTokenCookie(@NonNull HttpServletResponse response) {
        Cookie cookie = new Cookie(AT_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    // ─── Refresh Token Cookie ────────────────────────────────────────────────

    /**
     * 将 Refresh Token 写入 HttpOnly Cookie
     */
    public void setRefreshTokenCookie(@NonNull HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(RT_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath(COOKIE_PATH);
        cookie.setMaxAge(RT_MAX_AGE);
        response.addCookie(cookie);
    }

    /**
     * 清除 Refresh Token Cookie
     */
    public void clearRefreshTokenCookie(@NonNull HttpServletResponse response) {
        Cookie rtCookie = new Cookie(RT_COOKIE, "");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(secureCookie);
        rtCookie.setPath(COOKIE_PATH);
        rtCookie.setMaxAge(0);
        response.addCookie(rtCookie);
    }

    // ─── 通用方法 ────────────────────────────────────────────────────────────

    /**
     * 清除所有 Token Cookie（AT + RT）
     */
    public void clearAllTokenCookies(@NonNull HttpServletResponse response) {
        clearAccessTokenCookie(response);
        clearRefreshTokenCookie(response);
    }

    /**
     * 从请求 Cookie 中获取指定名称的 Cookie 值
     */
    public static @Nullable String getCookieValue(@NonNull HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
