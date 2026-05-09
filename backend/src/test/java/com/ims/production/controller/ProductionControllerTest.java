package com.ims.production.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import com.ims.production.dto.request.ProductionCreateRequest;
import com.ims.production.dto.request.ProductionUpdateRequest;
import com.ims.production.dto.request.SettlementUpdateRequest;
import com.ims.production.dto.response.ProductionResponse;
import com.ims.production.dto.response.SettlementResponse;
import com.ims.production.entity.SettlementResult;
import com.ims.production.entity.ProductionStatus;
import com.ims.production.service.ProductionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductionController.class)
@Import(SecurityConfig.class)
class ProductionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ProductionService productionService;
    @MockitoBean JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth(Long id) {
        return new UsernamePasswordAuthenticationToken(
                id, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private ProductionResponse productionResponse() {
        return new ProductionResponse(1L, 1L, 10L, "로드바이크", 5, ProductionStatus.PENDING, null, LocalDateTime.now());
    }

    private ProductionResponse settledResponse() {
        SettlementResponse settlement = new SettlementResponse(1L, SettlementResult.SUCCESS, null, null, LocalDateTime.now());
        return new ProductionResponse(1L, 1L, 10L, "로드바이크", 5, ProductionStatus.SETTLED, settlement, LocalDateTime.now());
    }

    @Test
    @DisplayName("생산 기록 등록 성공 - 201 Created")
    void createRecord_success() throws Exception {
        // given
        given(productionService.createRecord(eq(1L), eq(1L), any())).willReturn(productionResponse());

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/productions")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductionCreateRequest(10L, 5))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("생산 기록 등록 실패 - 미인증 401")
    void createRecord_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/productions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductionCreateRequest(10L, 5))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("생산 기록 취소 성공 - 200 OK")
    void cancelRecord_success() throws Exception {
        // given
        willDoNothing().given(productionService).cancelRecord(1L, 1L, 1L);

        // when & then
        mockMvc.perform(delete("/api/v1/warehouses/1/productions/1")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("생산 기록 목록 조회 성공 - 200 OK")
    void getRecords_success() throws Exception {
        // given
        PageImpl<ProductionResponse> page = new PageImpl<>(
                List.of(productionResponse()), PageRequest.of(0, 10), 1);
        given(productionService.getRecords(eq(1L), eq(1L), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1/productions")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("생산 기록 수정 성공 - 200 OK")
    void updateRecord_success() throws Exception {
        given(productionService.updateRecord(eq(1L), eq(1L), eq(1L), any())).willReturn(productionResponse());

        mockMvc.perform(patch("/api/v1/warehouses/1/productions/1")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductionUpdateRequest(30))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("강제 결산 성공 - 200 OK")
    void forceSettle_success() throws Exception {
        given(productionService.forceSettle(eq(1L), eq(1L), eq(1L))).willReturn(settledResponse());

        mockMvc.perform(post("/api/v1/warehouses/1/productions/1/settle")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SETTLED"));
    }

    @Test
    @DisplayName("결산 수정 성공 - 200 OK")
    void updateSettlement_success() throws Exception {
        given(productionService.updateSettlement(eq(1L), eq(1L), eq(1L), any())).willReturn(settledResponse());

        mockMvc.perform(patch("/api/v1/warehouses/1/productions/1/settlement")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SettlementUpdateRequest(SettlementResult.SUCCESS, "수정 메모"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlement.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("강제 결산 실패 - 미인증 401")
    void forceSettle_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/warehouses/1/productions/1/settle"))
                .andExpect(status().isUnauthorized());
    }
}
