package com.ims.production.entity;

import com.ims.global.common.BaseTimeEntity;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "production_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductionRecord extends BaseTimeEntity {

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

    /**
     * 생산 수량 수정
     */
    public void updateQuantity(int newQuantity) {
        if (this.status != ProductionStatus.PENDING) {
            throw new ImsException(ErrorCode.PRODUCTION_NOT_MODIFIABLE);
        }
        this.quantity = newQuantity;
    }

    /**
     * 생산 기록 취소
     */
    public void cancel() {
        if (this.status != ProductionStatus.PENDING) {
            throw new ImsException(ErrorCode.PRODUCTION_NOT_CANCELLABLE);
        }
        this.status = ProductionStatus.CANCELLED;
    }

    /**
     * 결산 완료 처리
     */
    public void settle() {
        this.status = ProductionStatus.SETTLED;
    }
}
