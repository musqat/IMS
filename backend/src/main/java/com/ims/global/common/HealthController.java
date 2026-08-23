package com.ims.global.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상태 확인 엔드포인트
 *
 * keepalive 워크플로가 핑하는 대상이다.
 * 기동 여부만 본다. DB·Redis 연결까지 확인하지 않는다.
 * keepalive는 프로세스를 깨우는 것이 목적이라 의존성 상태로 실패시킬 이유가 없다.
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("UP"));
    }
}
