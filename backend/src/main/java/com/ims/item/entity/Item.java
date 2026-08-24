package com.ims.item.entity;

import com.ims.global.common.BaseTimeEntity;
import com.ims.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "item_code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Item extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    private String description;

}
