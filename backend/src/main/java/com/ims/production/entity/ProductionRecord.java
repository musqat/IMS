package com.ims.production.entity;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "production_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProductionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductionStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 생산 기록 취소
     * - PENDING 상태가 아니면 PRODUCTION_NOT_CANCELLABLE
     */
    public void cancel() {
        if (this.status != ProductionStatus.PENDING) {
            throw new ImsException(ErrorCode.PRODUCTION_NOT_CANCELLABLE);
        }
        this.status = ProductionStatus.CANCELLED;
    }

    /**
     * 결산 완료 처리
     * - status = SETTLED
     */
    public void settle() {
        this.status = ProductionStatus.SETTLED;
    }
}
