package com.kevin.checkin.common;

import javax.servlet.http.HttpServletRequest;

/**
 * 从 HttpServletRequest 解析真实客户端 IP。
 * 部署在反向代理（如 nginx）之后时，getRemoteAddr() 拿到的是代理内网 IP，
 * 因此优先取 X-Forwarded-For / X-Real-IP 等标准头的最左（第一跳）地址。
 */
public final class IpUtil {

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For 形如 "client, proxy1, proxy2"，取第一段
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? null : remote.trim();
    }
}
