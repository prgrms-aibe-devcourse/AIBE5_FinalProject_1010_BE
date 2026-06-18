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
     * IPv6: 압축 표기(::)를 풀어 8그룹으로 정규화 후 앞 4 그룹 노출, 나머지 마스킹
     *       예) 2001:db8:85a3:0:****:****:****:****
     */
    private static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "Unknown";

        // 루프백 처리
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "localhost";
        }

        if (ip.contains(":")) {
            // :: 압축 표기를 0으로 채워 8그룹으로 정규화
            String expanded = expandIpv6(ip);
            String[] groups = expanded.split(":", -1);
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

    // "::" 압축 표기를 0 패딩으로 채워 8그룹 완전 표기로 변환
    private static String expandIpv6(String ip) {
        if (!ip.contains("::")) return ip;
        String[] halves = ip.split("::", -1);
        String left  = halves.length > 0 ? halves[0] : "";
        String right = halves.length > 1 ? halves[1] : "";
        int leftCount  = left.isEmpty()  ? 0 : left.split(":",  -1).length;
        int rightCount = right.isEmpty() ? 0 : right.split(":", -1).length;
        int zeroGroups = 8 - leftCount - rightCount;
        String zeros = "0:".repeat(zeroGroups);
        String middle = zeros.isEmpty() ? "" : zeros.substring(0, zeros.length() - 1);
        if (left.isEmpty() && right.isEmpty()) return middle;
        if (left.isEmpty())  return middle + ":" + right;
        if (right.isEmpty()) return left  + ":" + middle;
        return left + ":" + middle + ":" + right;
    }
}
