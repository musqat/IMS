package com.ims.inventory.entity;

import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.item.entity.Item;
import com.ims.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "item_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Inventory {

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

    @Column(nullable = false)
    private int safetyStock;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** 입고: quantity += qty */
    public void add(int qty) {
        quantity += qty;
    }

    /** 출고: quantity - qty < 0 이면 INSUFFICIENT_STOCK */
    public void deduct(int qty) {
        if (quantity - qty < 0) {
            throw new ImsException(ErrorCode.INSUFFICIENT_STOCK);
        }
        quantity -= qty;
    }

    /** 실사 보정: newQty < 0 이면 INVALID_QUANTITY */
    public void setQuantity(int newQty) {
        if (newQty < 0) {
            throw new ImsException(ErrorCode.INVALID_QUANTITY);
        }
        quantity = newQty;
    }

    /** 안전재고 이하 여부 */
    public boolean isBelowSafetyStock() {
        return quantity <= safetyStock;
    }

    /**
     * 안전재고 수정
     */
    public void updateSafetyStock(int newSafetyStock) {
        if (newSafetyStock < 0) throw new ImsException(ErrorCode.INVALID_QUANTITY);
        this.safetyStock = newSafetyStock;
    }
}
