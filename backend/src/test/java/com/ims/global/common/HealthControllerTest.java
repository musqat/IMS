package com.ims.global.common;

import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DependencyHealthService dependencyHealthService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("상태 확인은 토큰 없이 200을 준다")
    void healthWithoutToken() throws Exception {
        // given, when
        mockMvc.perform(get("/api/v1/health"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("UP"));
    }

    @Test
    @DisplayName("의존성 확인은 토큰 없이 200을 준다")
    void depsWithoutToken() throws Exception {
        // given
        given(dependencyHealthService.check()).willReturn(new DepsStatus("UP", "UP"));

        // when
        mockMvc.perform(get("/api/v1/health/deps"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data.db").value("UP"))
                .andExpect(jsonPath("$.data.redis").value("UP"));
    }

    @Test
    @DisplayName("DB가 죽으면 503을 준다")
    void depsDbDown() throws Exception {
        // given
        given(dependencyHealthService.check()).willReturn(new DepsStatus("DOWN", "UP"));

        // when
        mockMvc.perform(get("/api/v1/health/deps"))
                // then
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.db").value("DOWN"))
                .andExpect(jsonPath("$.data.redis").value("UP"));
    }

    @Test
    @DisplayName("Redis가 죽으면 503을 준다")
    void depsRedisDown() throws Exception {
        // given
        given(dependencyHealthService.check()).willReturn(new DepsStatus("UP", "DOWN"));

        // when
        mockMvc.perform(get("/api/v1/health/deps"))
                // then
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.redis").value("DOWN"));
    }
}
