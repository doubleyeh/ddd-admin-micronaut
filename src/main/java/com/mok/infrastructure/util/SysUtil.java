package com.mok.infrastructure.util;

import io.micronaut.http.HttpRequest;

import java.net.InetSocketAddress;

import static com.mok.infrastructure.common.Const.SUPER_ADMIN_USERNAME;
import static com.mok.infrastructure.common.Const.SUPER_TENANT_ID;

public final class SysUtil {

    public static boolean isSuperAdmin(String tenantId, String username) {
        return SUPER_TENANT_ID.equals(tenantId) && SUPER_ADMIN_USERNAME.equals(username);
    }

    public static boolean isSuperTenant(String tenantId) {
        return SUPER_TENANT_ID.equals(tenantId);
    }

    public static String getIpAddress(HttpRequest<?> request) {
        String ip = request.getHeaders().get("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        } else {
            ip = request.getHeaders().get("Proxy-Client-IP");
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            } else {
                ip = request.getHeaders().get("WL-Proxy-Client-IP");
                if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                } else {
                    InetSocketAddress remoteAddress = request.getRemoteAddress();
                    if (remoteAddress != null && remoteAddress.getAddress() != null) {
                        ip = remoteAddress.getAddress().getHostAddress();
                    } else {
                        ip = null;
                    }
                }
            }
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0] : ip;
    }

    public static String getBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";
        String ua = userAgent.toUpperCase();

        String os = "Unknown OS";
        if (ua.contains("WINDOWS")) os = "Windows";
        else if (ua.contains("ANDROID")) os = "Android";
        else if (ua.contains("IPHONE") || ua.contains("IPAD")) os = "iOS";
        else if (ua.contains("MACINTOSH") || ua.contains("MAC OS X")) os = "macOS";
        else if (ua.contains("LINUX")) os = "Linux";

        String browser = "Other";
        if (ua.contains("POSTMAN")) return os + " (Postman)";
        if (ua.contains("CURL")) return os + " (cURL)";
        if (ua.contains("MICROMESSENGER")) browser = "WeChat";
        else if (ua.contains("EDG/")) browser = "Edge";
        else if (ua.contains("CHROME/")) browser = "Chrome";
        else if (ua.contains("FIREFOX/")) browser = "Firefox";
        else if (ua.contains("SAFARI/") && !ua.contains("CHROME")) browser = "Safari";

        return os + " - " + browser;
    }
}
