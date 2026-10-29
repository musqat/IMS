package com.ims.global.common;

import com.ims.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class HealthController {

    private final DependencyHealthService dependencyHealthService;

    @GetMapping
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("UP"));
    }

    /**
     * DB·Redis까지 확인한다. 하나라도 죽으면 503
     *
     * 유휴 정지를 막는 쪽이 목적이라 호출 빈도는 하루 한 번으로 둔다.
     */
    @GetMapping("/deps")
    public ResponseEntity<ApiResponse<DepsStatus>> deps() {
        DepsStatus status = dependencyHealthService.check();

        if (status.allUp()) {
            return ResponseEntity.ok(ApiResponse.success(status));
        }

        // 어느 쪽이 죽었는지 응답에 남긴다
        ErrorCode errorCode = ErrorCode.DEPENDENCY_UNAVAILABLE;
        return ResponseEntity.status(errorCode.getStatus())
                .body(new ApiResponse<>(errorCode.name(), errorCode.getMessage(), status));
    }
}
