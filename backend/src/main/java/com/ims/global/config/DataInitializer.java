package com.ims.global.config;

import com.ims.user.dto.request.RegisterRequest;
import com.ims.user.repository.UserRepository;
import com.ims.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.existsByEmail("a@ims.dev")) {
            log.info("[DataInitializer] Seed data already exists. Skipping.");
            return;
        }

        log.info("[DataInitializer] Applying seed data...");

        // A: 메인 데모 계정 (조립사), B/C: A의 하청, D: 물류(초대 대기), E: A의 본사
        userService.signUp(new RegisterRequest("a@ims.dev", "Test1234!", "아이테크조립(주)"));
        userService.signUp(new RegisterRequest("b@ims.dev", "Test1234!", "비전전자(주)"));
        userService.signUp(new RegisterRequest("c@ims.dev", "Test1234!", "씨메카닉스(주)"));
        userService.signUp(new RegisterRequest("d@ims.dev", "Test1234!", "디로지스(주)"));
        userService.signUp(new RegisterRequest("e@ims.dev", "Test1234!", "이스마트코리아(주)"));

        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("seed.sql"));
        }

        log.info("[DataInitializer] Seed data applied successfully.");
    }
}
