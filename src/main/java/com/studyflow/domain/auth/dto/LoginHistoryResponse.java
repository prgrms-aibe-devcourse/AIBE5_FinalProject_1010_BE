package com.studyflow.domain.auth.dto;

import com.studyflow.domain.auth.entity.LoginHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class LoginHistoryResponse {

    private final Long id;
    private final LocalDateTime loginAt;
    private final String ipAddress;
    private final String deviceInfo;
    private final String browser;

    private LoginHistoryResponse(LoginHistory history) {
        this.id = history.getId();
        this.loginAt = history.getLoginAt();
        this.ipAddress = maskIp(history.getIpAddress());
        this.deviceInfo = history.getDeviceInfo();
        this.browser = history.getBrowser();
    }

    public static LoginHistoryResponse from(LoginHistory history) {
        return new LoginHistoryResponse(history);
    }

    /**
     * IPv4 루프백(127.0.0.1): "localhost"
     * IPv6 루프백(::1 / 0:0:0:0:0:0:0:1): "localhost"
     * IPv4: 앞 2 옥텟 노출, 나머지 마스킹  예) 192.168.***.***
     * IPv6: 앞 4 그룹 노출, 나머지 마스킹  예) 2001:db8:85a3:0:****:****:****:****
     */
    private static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "Unknown";

        // 루프백 처리
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "localhost";
        }

        if (ip.contains(":")) {
            // IPv6
            String[] groups = ip.split(":", -1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < groups.length; i++) {
                if (i > 0) sb.append(":");
                sb.append(i < 4 ? groups[i] : "****");
            }
            return sb.toString();
        }

        // IPv4
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) return ip;
        return octets[0] + "." + octets[1] + ".***.***";
    }
}
