package com.studyflow.global.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ALB 대상그룹 헬스체크용 경량 엔드포인트 (멀티 인스턴스 배포).
 *
 * <p>actuator를 추가하지 않고, 인증 없이 200을 반환하는 단일 경로만 제공한다.
 * ALB는 이 경로로 각 EC2의 앱 컨테이너 생존을 판단한다(DB/Redis 접근 없이 프로세스 응답성만 확인).</p>
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
