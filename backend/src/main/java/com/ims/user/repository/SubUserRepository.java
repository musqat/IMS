package com.ims.user.repository;

import com.ims.user.entity.SubUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubUserRepository extends JpaRepository<SubUser, Long> {

    // 같은 USER 내에서 loginId 중복 체크
    boolean existsByUserIdAndLoginId(Long userId, String loginId);

    // companyCode + loginId 로 SubUser 조회 (로그인 시 사용)
    Optional<SubUser> findByUser_CompanyCodeAndLoginId(String companyCode, String loginId);

    List<SubUser> findAllByUserId(Long userId);
}
