package com.ims.partnership.entity;

import com.ims.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "partnerships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"main_id", "sub_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Partnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_id", nullable = false)
    private User main;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_id", nullable = false)
    private User sub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartnershipStatus status;

    @Column(unique = true)
    private String inviteToken;

    @Column
    private String alias;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime acceptedAt;

    public enum PartnershipStatus {
        PENDING, ACCEPTED
    }

    public void accept() {
        this.status = PartnershipStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        this.inviteToken = null;
    }

    public void updateAlias(String alias) {
        this.alias = alias;
    }
}
