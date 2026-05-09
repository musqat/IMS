package com.ims.production.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_record_id", nullable = false, unique = true)
    private ProductionRecord productionRecord;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementResult result;

    /** ANOMALY일 때 부족 부품 정보 JSON, SUCCESS면 null */
    @Column(columnDefinition = "TEXT")
    private String anomalyDetail;

    /** 수동 수정 메모 */
    @Column(columnDefinition = "TEXT")
    private String memo;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime settledAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 결산 결과 수동 수정 */
    public void update(SettlementResult newResult, String newMemo) {
        this.result = newResult;
        this.memo = newMemo;
    }
}
