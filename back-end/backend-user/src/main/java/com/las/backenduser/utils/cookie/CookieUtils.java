package com.las.backenduser.utils.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {
    private CookieUtils() {
        // INOP
    }

    public static final String RT_COOKIE = "refresh_token";

    private static final int RT_MAX_AGE = 14 * 24 * 60 * 60; // 14 天（秒）

    /**
     * 将 Refresh Token 写入 HttpOnly Cookie
     */
    public static void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(RT_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(RT_MAX_AGE);
        response.addCookie(cookie);
    }

    /**
     * 清除 Refresh Token Cookie
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie rtCookie = new Cookie(RT_COOKIE, "");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(true);
        rtCookie.setPath("/");
        rtCookie.setMaxAge(0);
        response.addCookie(rtCookie);
    }

    /**
     * 从请求 Cookie 中获取指定名称的 Cookie 值
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
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
