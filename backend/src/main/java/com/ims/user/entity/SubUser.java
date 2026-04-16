package com.ims.user.entity;

import com.ims.global.common.Role;
import jakarta.persistence.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "sub_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "login_id"})
)
public class SubUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // PRODUCTION 또는 WAREHOUSE

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }
}
