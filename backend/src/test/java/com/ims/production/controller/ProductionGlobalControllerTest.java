package com.ims.production.controller;

import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import com.ims.production.dto.response.ProductionCountsResponse;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.service.ProductionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductionGlobalController.class)
@Import(SecurityConfig.class)
class ProductionGlobalControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProductionService productionService;
    @MockitoBean JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth(Long id) {
        return new UsernamePasswordAuthenticationToken(
                id, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private ProductionResponse pendingResponse() {
        return new ProductionResponse(1L, 1L, 10L, "로드바이크", 5,
                ProductionStatus.PENDING, null, LocalDateTime.now());
    }

    @Test
    @DisplayName("상태 필터 조회 - PENDING 기록 반환")
    void getRecordsByStatus_returnsPendingRecords() throws Exception {
        // given
        given(productionService.getRecordsByStatus(eq(1L), eq(ProductionStatus.PENDING), any()))
                .willReturn(new PageImpl<>(List.of(pendingResponse()), PageRequest.of(0, 30), 1));

        // when & then
        mockMvc.perform(get("/api/v1/productions")
                        .param("status", "PENDING")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("상태 필터 조회 - status 파라미터 누락 시 400")
    void getRecordsByStatus_missingStatusParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/productions")
                        .with(authentication(auth(1L))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상태별 카운트 조회 - 집계 결과 반환")
    void getCounts_returnsStatusCounts() throws Exception {
        // given
        ProductionCountsResponse counts = new ProductionCountsResponse(3L, 10L, 2L, 1L);
        given(productionService.getStatusCounts(1L)).willReturn(counts);

        // when & then
        mockMvc.perform(get("/api/v1/productions/counts")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending").value(3))
                .andExpect(jsonPath("$.data.settled").value(10))
                .andExpect(jsonPath("$.data.cancelled").value(2))
                .andExpect(jsonPath("$.data.anomaly").value(1));
    }

    @Test
    @DisplayName("인증 없이 카운트 조회 시 401")
    void getCounts_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/productions/counts"))
                .andExpect(status().isUnauthorized());
    }
}
