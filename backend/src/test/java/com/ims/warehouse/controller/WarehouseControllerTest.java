package com.ims.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.warehouse.dto.request.ShareRequest;
import com.ims.warehouse.dto.request.WarehouseCreateRequest;
import com.ims.warehouse.dto.response.WarehouseResponse;
import com.ims.warehouse.dto.response.WarehouseShareResponse;
import com.ims.warehouse.entity.WarehouseShare.SharePermission;
import com.ims.warehouse.service.WarehouseService;
import com.ims.warehouse.service.WarehouseShareService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(WarehouseController.class)
@Import(SecurityConfig.class)
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WarehouseService warehouseService;

    @MockitoBean
    private WarehouseShareService warehouseShareService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth(Long id) {
        return new UsernamePasswordAuthenticationToken(
                id, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    private WarehouseResponse warehouseResponse() {
        return new WarehouseResponse(1L, "서울 창고", "서울시 강남구", 1L, "테스트회사", true, LocalDateTime.now());
    }

    @Test
    @DisplayName("창고 생성 성공")
    void create_success() throws Exception {
        // given
        given(warehouseService.createWarehouse(eq(1L), any())).willReturn(warehouseResponse());

        // when & then
        mockMvc.perform(post("/api/v1/warehouses")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WarehouseCreateRequest("서울 창고", "서울시 강남구"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("서울 창고"));
    }

    @Test
    @DisplayName("창고 생성 실패 - 입력값 오류")
    void create_invalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/warehouses")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WarehouseCreateRequest("", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("창고 목록 조회 성공")
    void getList_success() throws Exception {
        // given
        given(warehouseService.getWarehouses(1L)).willReturn(List.of(warehouseResponse()));

        // when & then
        mockMvc.perform(get("/api/v1/warehouses")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("서울 창고"));
    }

    @Test
    @DisplayName("창고 단건 조회 성공")
    void getOne_success() throws Exception {
        // given
        given(warehouseService.getWarehouse(1L, 1L)).willReturn(warehouseResponse());

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/1")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("서울 창고"));
    }

    @Test
    @DisplayName("창고 삭제 성공")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/v1/warehouses/1")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());

        then(warehouseService).should().deleteWarehouse(1L, 1L);
    }

    @Test
    @DisplayName("창고 삭제 실패 - 소유자 아님")
    void delete_notOwner() throws Exception {
        // given
        willThrow(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED))
                .given(warehouseService).deleteWarehouse(2L, 1L);

        // when & then
        mockMvc.perform(delete("/api/v1/warehouses/1")
                        .with(authentication(auth(2L))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("창고 공유 성공")
    void share_success() throws Exception {
        // given
        WarehouseShareResponse response = new WarehouseShareResponse(1L, 1L, "서울 창고", "서울시 강남구",1L ,"하청", 1L, "테스트회사 ","VIEW");
        given(warehouseShareService.share(eq(1L), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/shares")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShareRequest("2000000001", SharePermission.VIEW))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.permission").value("VIEW"));
    }

    @Test
    @DisplayName("창고 공유 실패 - Partnership 관계 없음")
    void share_notPartner() throws Exception {
        // given
        given(warehouseShareService.share(eq(1L), eq(1L), any()))
                .willThrow(new ImsException(ErrorCode.NOT_PARTNER));

        // when & then
        mockMvc.perform(post("/api/v1/warehouses/1/shares")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ShareRequest("2000000001", SharePermission.VIEW))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("공유 회수 성공")
    void revoke_success() throws Exception {
        mockMvc.perform(delete("/api/v1/warehouses/1/shares")
                        .with(authentication(auth(1L)))
                        .param("companyCode", "2000000001"))
                .andExpect(status().isOk());

        then(warehouseShareService).should().revoke(1L, 1L, "2000000001");
    }

    @Test
    @DisplayName("공유받은 창고 목록 조회 성공")
    void getShared_success() throws Exception {
        // given
        WarehouseShareResponse response = new WarehouseShareResponse(1L, 1L, "서울 창고", "서울시 강남구",1L ,"하청", 1L, "테스트회사 ","VIEW");
        given(warehouseShareService.getSharedWarehouses(2L)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/warehouses/shared")
                        .with(authentication(auth(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].warehouseName").value("서울 창고"));
    }
}
