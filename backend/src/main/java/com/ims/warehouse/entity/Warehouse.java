package com.ims.warehouse.entity;

import com.ims.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    private String location;

    /**
     * 사용 여부. false면 목록에서 숨기고 입출고·생산을 차단한다.
     * 재고나 생산 기록이 있으면 물리 삭제를 할 수 없어(분석의 원본이다)
     * 대신 닫는 방식을 쓴다. 과거 이력 조회는 계속 가능하다
     */
    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 창고를 비활성화한다. 이력은 그대로 두고 목록·쓰기에서만 제외된다 */
    public void deactivate() {
        this.active = false;
    }

    /** 비활성 창고를 다시 활성화한다 */
    public void activate() {
        this.active = true;
    }
}
