package com.ims.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.item.dto.request.ItemCreateRequest;
import com.ims.item.dto.response.ItemResponse;
import com.ims.item.entity.ItemType;
import com.ims.item.service.ItemService;
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

@WebMvcTest(ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private ItemResponse itemResponse() {
        return new ItemResponse(1L, "ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
    }

    @Test
    @DisplayName("품목 생성 성공")
    void createItem_success() throws Exception {
        ItemCreateRequest request = new ItemCreateRequest("ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(itemService.createItem(eq(1L), any())).willReturn(itemResponse());

        mockMvc.perform(post("/api/v1/items")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.itemCode").value("ITEM-001"))
                .andExpect(jsonPath("$.data.name").value("테스트 품목"))
                .andExpect(jsonPath("$.data.type").value("PRODUCT"));
    }

    @Test
    @DisplayName("품목 생성 실패 - 중복 itemCode")
    void createItem_duplicateCode() throws Exception {
        ItemCreateRequest request = new ItemCreateRequest("ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(itemService.createItem(eq(1L), any())).willThrow(new ImsException(ErrorCode.DUPLICATE_ITEM_CODE));

        mockMvc.perform(post("/api/v1/items")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("품목 목록 조회 성공")
    void getItems_success() throws Exception {
        ItemResponse response = new ItemResponse(1L, "ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(itemService.getItems(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/items")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemCode").value("ITEM-001"));
    }

    @Test
    @DisplayName("품목 단건 조회 성공")
    void getItem_success() throws Exception {
        ItemResponse response = new ItemResponse(1L, "ITEM-001", "테스트 품목", ItemType.PRODUCT, null);
        given(itemService.getItem(1L, 1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/items/1")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCode").value("ITEM-001"));
    }

    @Test
    @DisplayName("품목 단건 조회 실패 - 소유자 아님")
    void getItem_notOwner() throws Exception {
        given(itemService.getItem(1L, 1L)).willThrow(new ImsException(ErrorCode.ITEM_NOT_OWNED));

        mockMvc.perform(get("/api/v1/items/1")
                        .with(authentication(auth())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("품목 삭제 성공")
    void deleteItem_success() throws Exception {
        mockMvc.perform(delete("/api/v1/items/1")
                        .with(authentication(auth())))
                .andExpect(status().isOk());
    }
}
