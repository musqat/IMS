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

    /**
     * 초대 토큰 만료 시각
     * - createdAt은 @CreatedDate라 재초대 시 갱신되지 않아 별도로 둔다
     * - 수락하면 토큰과 함께 비운다
     */
    private LocalDateTime inviteExpiresAt;

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
        this.inviteExpiresAt = null;
    }

    /** 초대가 만료됐는지. 만료 시각이 없으면 만료로 보지 않는다(기존 데이터 호환) */
    public boolean isInviteExpired() {
        return inviteExpiresAt != null && inviteExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * 초대 토큰 재발급
     * - UK가 (main_id, sub_id)라 만료된 초대에 새 행을 만들 수 없다.
     *   기존 행의 토큰과 만료 시각을 새로 발급해 재초대를 가능하게 한다
     */
    public void reissueInvite(String token, LocalDateTime expiresAt) {
        this.inviteToken = token;
        this.inviteExpiresAt = expiresAt;
    }

    public void updateAlias(String alias) {
        this.alias = alias;
    }
}
