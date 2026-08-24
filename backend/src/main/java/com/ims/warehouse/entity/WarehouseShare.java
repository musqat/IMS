package com.ims.warehouse.entity;

import com.ims.global.common.BaseTimeEntity;
import com.ims.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "warehouse_shares",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "shared_with_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WarehouseShare extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_id", nullable = false)
    private User sharedWith;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermission permission;

    public enum SharePermission {
        VIEW, FULL
    }

    public void updatePermission(SharePermission permission) {
        this.permission = permission;
    }
}
