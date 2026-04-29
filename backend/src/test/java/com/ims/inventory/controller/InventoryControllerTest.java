package com.ims.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.security.JwtProvider;
import com.ims.inventory.dto.request.InventoryCreateRequest;
import com.ims.inventory.dto.request.AdjustRequest;
import com.ims.inventory.dto.request.InboundRequest;
import com.ims.inventory.dto.request.OutboundRequest;
import com.ims.inventory.dto.response.InventoryHistoryResponse;
import com.ims.inventory.dto.response.InventoryResponse;
import com.ims.inventory.dto.response.MaxProducibleResponse;
import com.ims.inventory.entity.InventoryHistoryType;
import com.ims.inventory.service.InventoryService;
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
        return new InventoryResponse(1L, 1L, 10L, "ITEM-001", "자전거", 100, 20, null);
    }

    @Test
    @DisplayName("재고 항목 등록 성공 - 201 Created")
    void createInventory_success() throws Exception {
        given(inventoryService.createInventory(eq(1L), eq(1L), any())).willReturn(inventoryResponse());

        mockMvc.perform(post("/api/v1/warehouses/1/inventories")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InventoryCreateRequest(10L, 20))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("입고 성공 - 200 OK")
    void adjustIn_success() throws Exception {
        given(inventoryService.adjustIn(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/in")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InboundRequest(50, "입고 메모"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("입고 실패 - 미인증 401 Unauthorized")
    void adjustIn_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InboundRequest(50, "입고 메모"))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("출고 성공 - 200 OK")
    void adjustOut_success() throws Exception {
        given(inventoryService.adjustOut(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        mockMvc.perform(post("/api/v1/warehouses/1/inventories/10/out")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OutboundRequest(30, "출고 메모"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("절대값 보정 성공 - 200 OK")
    void adjust_success() throws Exception {
        given(inventoryService.adjust(eq(1L), eq(1L), eq(10L), any())).willReturn(inventoryResponse());

        mockMvc.perform(put("/api/v1/warehouses/1/inventories/10/adjust")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdjustRequest(80, "보정 메모"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재고 목록 조회 성공 - 200 OK")
    void getInventories_success() throws Exception {
        PageImpl<InventoryResponse> page = new PageImpl<>(
                List.of(inventoryResponse()),
                PageRequest.of(0, 10),
                1
        );
        given(inventoryService.getInventories(eq(1L), eq(1L), eq("자전"), any())).willReturn(page);

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
        InventoryHistoryResponse historyResponse = new InventoryHistoryResponse(
                1L, InventoryHistoryType.IN, 50, "입고 메모", LocalDateTime.now()
        );
        PageImpl<InventoryHistoryResponse> page = new PageImpl<>(
                List.of(historyResponse),
                PageRequest.of(0, 10),
                1
        );
        given(inventoryService.getHistory(eq(1L), eq(1L), eq(10L), any())).willReturn(page);

        mockMvc.perform(get("/api/v1/warehouses/1/inventories/10/history")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("최대 생산 가능 수량 조회 성공 - 200 OK")
    void calcMaxProducible_success() throws Exception {
        MaxProducibleResponse response = new MaxProducibleResponse(10L, "자전거", 5);
        given(inventoryService.calcMaxProducible(eq(1L), eq(1L), eq(10L))).willReturn(response);

        mockMvc.perform(get("/api/v1/warehouses/1/inventories/10/max-producible")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }
}
