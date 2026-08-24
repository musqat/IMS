package com.ims.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.item.dto.request.BomCreateRequest;
import com.ims.item.dto.response.BomResponse;
import com.ims.item.service.BomService;
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

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BomController.class)
@Import(SecurityConfig.class)
class BomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BomService bomService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private BomResponse bomResponse() {
        return new BomResponse(10L, 1L, "A", "완성품A", 2L, "B", "반제품B", 2);
    }

    @Test
    @DisplayName("BOM 등록 성공")
    void addBom_success() throws Exception {
        // given
        BomCreateRequest request = new BomCreateRequest(1L, 2);
        given(bomService.addBom(1L, 1L, request)).willReturn(bomResponse());

        // when & then
        mockMvc.perform(post("/api/v1/items/{parentItemId}/bom", 1)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.childItemId").value(2))
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @DisplayName("BOM 등록 실패 - 순환 참조")
    void addBom_circularReference() throws Exception {
        // given
        BomCreateRequest request = new BomCreateRequest(1L, 2);
        given(bomService.addBom(1L, 1L, request)).willThrow(new ImsException(ErrorCode.BOM_CIRCULAR_REFERENCE));

        // when & then
        mockMvc.perform(post("/api/v1/items/{parentItemId}/bom", 1)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BOM 목록 조회 성공")
    void getBoms_success() throws Exception {
        // given
        BomResponse response = new BomResponse(10L, 1L, "A", "완성품A", 2L, "B", "반제품B", 2);
        given(bomService.getBoms(1L, 1L)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/items/1/bom")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].childItemId").value(2));
    }

    @Test
    @DisplayName("BOM 삭제 성공")
    void deleteBom_success() throws Exception {
        mockMvc.perform(delete("/api/v1/items/1/bom/1")
                        .with(authentication(auth())))
                .andExpect(status().isOk());
    }
}
