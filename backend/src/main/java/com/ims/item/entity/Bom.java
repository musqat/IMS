package com.ims.item.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "boms",
    uniqueConstraints = @UniqueConstraint(columnNames = {"parent_item_id", "child_item_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Bom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_item_id", nullable = false)
    private Item parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_item_id", nullable = false)
    private Item child;

    /** 상위 품목 1개 생산에 필요한 하위 품목 수량 */
    @Column(nullable = false)
    private int quantity;
}
