package com.ims.user.entity;

import com.ims.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String companyName;

    @Column(unique = true, nullable = false, length = 10)
    private String companyCode;

    public static User register(String email, String rawPassword, String companyName,
                                String companyCode, PasswordEncoder encoder) {
        return User.builder()
                .email(email)
                .password(encoder.encode(rawPassword))
                .companyName(companyName)
                .companyCode(companyCode)
                .build();
    }

    public boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }

    public void updateCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
