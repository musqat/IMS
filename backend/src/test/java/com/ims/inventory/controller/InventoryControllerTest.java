package com.ims.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.request.SafetyStockUpdateRequest;
import com.ims.inventory.dto.response.InventoryExportRow;
import com.ims.inventory.dto.response.InventoryHistoryResponse;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.dto.response.ShortageItemResponse;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.service.InventoryService;
import com.ims.item.entity.ItemType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth(Long id) {
        return new UsernamePasswordAuthenticationToken(
                id, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private InventoryResponse inventoryResponse() {
        return new InventoryResponse(1L, 1L, 10L, "ITEM-001", "자전거", ItemType.PART,100, 20, null);
    }

    @Test
    @DisplayName("재고 항목 등록 성공 - 201 Created")
    void createInventory_success() throws Exception {
        // given
        given(inventoryService.createInventory(eq(1L), eq(1L), any())).willReturn(inventoryResponse());

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/inventories")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InventoryCreateRequest(10L, 20, 15))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("입고 성공 - 200 OK, 응답 body itemCode·quantity 검증")
    void adjustIn_success() throws Exception {
        // given
        given(inventoryService.adjustIn(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/in")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InboundRequest(50, "입고 메모"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCode").value("ITEM-001"))
                .andExpect(jsonPath("$.data.quantity").value(100));
    }

    @Test
    @DisplayName("출고 실패 - 재고 부족 시 400 Bad Request")
    void adjustOut_insufficientStock_returns400() throws Exception {
        // given
        given(inventoryService.adjustOut(eq(1L), eq(1L), eq(10L), any()))
                .willThrow(new ImsException(ErrorCode.INSUFFICIENT_STOCK));

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/out")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OutboundRequest(9999, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("재고가 부족합니다."));
    }

    @Test
    @DisplayName("입고 실패 - 미인증 401 Unauthorized")
    void adjustIn_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InboundRequest(50, "입고 메모"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("입고 실패 - 쓰기 권한 없으면 403 Forbidden")
    void adjustIn_noWritePermission_returns403() throws Exception {
        // given
        // 인증은 됐지만 해당 창고에 대한 쓰기 권한이 없는 경우 (VIEW 공유 또는 무관계)
        given(inventoryService.adjustIn(eq(2L), eq(1L), eq(10L), any()))
                .willThrow(new ImsException(ErrorCode.WAREHOUSE_ACCESS_DENIED));

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/in")
                        .with(authentication(auth(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InboundRequest(50, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("해당 창고에 대한 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("출고 성공 - 200 OK")
    void adjustOut_success() throws Exception {
        // given
        given(inventoryService.adjustOut(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/out")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OutboundRequest(30, "출고 메모"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("절대값 보정 성공 - 200 OK")
    void adjust_success() throws Exception {
        // given
        given(inventoryService.adjust(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        // when & then
        mockMvc.perform(put("/api/v1/warehouses/1/inventories/10/adjust")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustRequest(80, "보정 메모"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재고 목록 조회 성공 - 200 OK")
    void getInventories_success() throws Exception {
        // given
        PageImpl<InventoryResponse> page = new PageImpl<>(
                List.of(inventoryResponse()),
                PageRequest.of(0, 10),
                1
        );
        given(inventoryService.getInventories(eq(1L), eq(1L), eq("자전"), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1/inventories")
                        .with(authentication(auth(1L)))
                        .param("keyword", "자전")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입출고 이력 조회 성공 - 200 OK")
    void getHistory_success() throws Exception {
        // given
        InventoryHistoryResponse historyResponse = new InventoryHistoryResponse(
                1L, InventoryHistoryType.IN, 50, "입고 메모", LocalDateTime.now()
        );
        PageImpl<InventoryHistoryResponse> page = new PageImpl<>(
                List.of(historyResponse),
                PageRequest.of(0, 10),
                1
        );
        given(inventoryService.getHistory(eq(1L), eq(1L), eq(10L), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/10/history")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("최대 생산 가능 수량 조회 성공 - 200 OK")
    void calcMaxProducible_success() throws Exception {
        // given
        MaxProducibleResponse response = new MaxProducibleResponse(10L, "자전거", 5);
        given(inventoryService.calcMaxProducible(eq(1L), eq(1L), eq(10L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/10/max-producible")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("안전재고 수정 성공 - 200 OK")
    void updateSafetyStock_success() throws Exception {
        // given
        given(inventoryService.updateSafetyStock(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        // when & then
        mockMvc.perform(patch("/api/v1/warehouses/1/inventories/10/safety-stock")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SafetyStockUpdateRequest(30))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("안전재고 수정 실패 - 미인증 401")
    void updateSafetyStock_unauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/warehouses/1/inventories/10/safety-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SafetyStockUpdateRequest(30))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("부족 재고 분석 조회 성공 - 200 OK")
    void getShortageAnalysis_success() throws Exception {
        // given
        List<ShortageItemResponse> responses = List.of(
                new ShortageItemResponse(10L, "BIKE-001", "자전거", List.of())
        );
        given(inventoryService.getShortageAnalysis(eq(1L), eq(1L))).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/shortage-analysis")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("부족 재고 분석 조회 실패 - 미인증 401")
    void getShortageAnalysis_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/shortage-analysis"))
                .andExpect(status().isUnauthorized());
    }

    // ===================== 이력 Export (엑셀) =====================

    @Test
    @DisplayName("이력 Export 조회 성공 - 200 OK")
    void getWarehouseHistory_success() throws Exception {
        // given
        given(inventoryService.getWarehouseHistory(eq(1L), eq(1L), any(), any(), any()))
                .willReturn(List.of(new InventoryExportRow(
                        "P001", "PCB기판", InventoryHistoryType.IN, 100, LocalDate.of(2026, 8, 1))));

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/histories")
                        .param("types", "IN")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-10")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemCode").value("P001"))
                .andExpect(jsonPath("$.data[0].delta").value(100));
    }

    @Test
    @DisplayName("이력 Export 조회 실패 - 시작일이 종료일보다 늦으면 400")
    void getWarehouseHistory_fromAfterTo_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/histories")
                        .param("types", "IN")
                        .param("from", "2026-08-10")
                        .param("to", "2026-08-01")
                        .with(authentication(auth(1L))))
                .andExpect(status().isBadRequest());

        then(inventoryService).should(never())
                .getWarehouseHistory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이력 Export 조회 실패 - 조회 기간이 1년을 넘으면 400")
    void getWarehouseHistory_rangeTooLarge_badRequest() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/histories")
                        .param("types", "IN")
                        .param("from", "2025-01-01")
                        .param("to", "2026-08-10")
                        .with(authentication(auth(1L))))
                .andExpect(status().isBadRequest());

        then(inventoryService).should(never())
                .getWarehouseHistory(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이력 Export 조회 - 정확히 1년은 허용된다 (경계값)")
    void getWarehouseHistory_exactlyOneYear_ok() throws Exception {
        // given
        given(inventoryService.getWarehouseHistory(any(), any(), any(), any(), any()))
                .willReturn(List.of());

        // when & then
        // from.plusYears(1).isBefore(to) 가 false여야 통과한다.
        // 프런트의 '최근 1년' 프리셋이 정확히 이 경계를 만들어내므로 반드시 열려 있어야 한다.
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/histories")
                        .param("types", "IN")
                        .param("from", "2025-08-10")
                        .param("to", "2026-08-10")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("이력 Export 조회 실패 - 미인증 401")
    void getWarehouseHistory_unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/1/inventories/histories")
                        .param("from", "2026-08-01")
                        .param("to", "2026-08-10"))
                .andExpect(status().isUnauthorized());
    }
}
