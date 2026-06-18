package com.studyflow.global.util;

import jakarta.servlet.http.HttpServletRequest;

public class UserAgentParser {

    private UserAgentParser() {}

    public static String extractClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public static String extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";

        // OS 감지
        if (userAgent.contains("Windows NT")) {
            String version = extractWindowsVersion(userAgent);
            return isMobile(userAgent) ? "Windows " + version + " / Mobile" : "Windows " + version + " / PC";
        }
        if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS X")) {
            return isMobile(userAgent) ? "macOS / Mobile" : "macOS / PC";
        }
        if (userAgent.contains("iPhone")) return "iOS / iPhone";
        if (userAgent.contains("iPad"))  return "iOS / iPad";
        if (userAgent.contains("Android")) {
            String version = extractBetween(userAgent, "Android ", ";");
            return "Android " + (version.isEmpty() ? "" : version).trim() + " / Mobile";
        }
        if (userAgent.contains("Linux")) return "Linux / PC";
        if (userAgent.contains("CrOS"))  return "ChromeOS / PC";

        return "Unknown";
    }

    public static String extractBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Unknown";

        // 순서 중요: 더 구체적인 것 먼저
        if (userAgent.contains("Edg/") || userAgent.contains("Edge/")) return "Edge";
        if (userAgent.contains("OPR/") || userAgent.contains("Opera/")) return "Opera";
        if (userAgent.contains("SamsungBrowser/")) return "Samsung Internet";
        if (userAgent.contains("Chrome/") && !userAgent.contains("Chromium/")) return "Chrome";
        if (userAgent.contains("Chromium/")) return "Chromium";
        if (userAgent.contains("Firefox/")) return "Firefox";
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) return "Safari";
        if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) return "Internet Explorer";

        return "Unknown";
    }

    private static boolean isMobile(String userAgent) {
        return userAgent.contains("Mobile") || userAgent.contains("Android");
    }

    private static String extractWindowsVersion(String userAgent) {
        String raw = extractBetween(userAgent, "Windows NT ", ";");
        if (raw.isEmpty()) raw = extractBetween(userAgent, "Windows NT ", ")");
        return switch (raw.trim()) {
            case "10.0" -> "10/11";
            case "6.3"  -> "8.1";
            case "6.2"  -> "8";
            case "6.1"  -> "7";
            default     -> raw.trim();
        };
    }

    private static String extractBetween(String source, String start, String end) {
        int s = source.indexOf(start);
        if (s < 0) return "";
        s += start.length();
        int e = source.indexOf(end, s);
        if (e < 0) return source.substring(s);
        return source.substring(s, e);
    }
}
