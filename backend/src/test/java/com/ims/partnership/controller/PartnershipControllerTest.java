package com.ims.partnership.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ims.global.config.SecurityConfig;
import com.ims.global.exception.ErrorCode;
import com.ims.global.exception.ImsException;
import com.ims.global.security.JwtProvider;
import com.ims.partnership.dto.request.AliasRequest;
import com.ims.partnership.dto.request.InviteRequest;
import com.ims.partnership.dto.response.InviteResponse;
import com.ims.partnership.dto.response.PartnershipResponse;
import com.ims.partnership.service.PartnershipService;
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

@WebMvcTest(PartnershipController.class)
@Import(SecurityConfig.class)
class PartnershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PartnershipService partnershipService;

    @MockitoBean
    private JwtProvider jwtProvider;

    private UsernamePasswordAuthenticationToken auth(Long id) {
        return new UsernamePasswordAuthenticationToken(
                id, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Test
    @DisplayName("초대 발송 성공")
    void invite_success() throws Exception {
        // given
        InviteRequest request = new InviteRequest("2000000001");
        InviteResponse inviteResponse = new InviteResponse(1L, "invite-token-uuid");
        given(partnershipService.invite(eq(1L), any())).willReturn(inviteResponse);

        // when & then
        mockMvc.perform(post("/api/v1/partnerships/invite")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteToken").value("invite-token-uuid"));
    }

    @Test
    @DisplayName("초대 발송 실패 - 이미 존재하는 파트너십")
    void invite_duplicatePartnership() throws Exception {
        // given
        InviteRequest request = new InviteRequest("2000000001");
        given(partnershipService.invite(eq(1L), any()))
                .willThrow(new ImsException(ErrorCode.DUPLICATE_PARTNERSHIP));

        // when & then
        mockMvc.perform(post("/api/v1/partnerships/invite")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("초대 수락 성공")
    void accept_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(1L, 1L, "본사", 2L, "하청", "SUB001", "ACCEPTED", null, null, null);
        given(partnershipService.accept(eq(2L), eq("valid-token"))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/partnerships/accept")
                        .with(authentication(auth(2L)))
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("초대 수락 실패 - 유효하지 않은 토큰")
    void accept_invalidToken() throws Exception {
        // given
        given(partnershipService.accept(eq(2L), eq("bad-token")))
                .willThrow(new ImsException(ErrorCode.INVALID_INVITE_TOKEN));

        // when & then
        mockMvc.perform(post("/api/v1/partnerships/accept")
                        .with(authentication(auth(2L)))
                        .param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("하청 목록 조회 성공")
    void getSubList_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(1L, 1L, "본사", 2L, "하청", "SUB001", "ACCEPTED", null, null, null);
        given(partnershipService.getSubList(1L)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/partnerships/subs")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subCompanyName").value("하청"));
    }

    @Test
    @DisplayName("본사 목록 조회 성공")
    void getMainList_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(1L, 1L, "본사", 2L, "하청", "SUB001", "ACCEPTED", null, null, null);
        given(partnershipService.getMainList(2L)).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/partnerships/mains")
                        .with(authentication(auth(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mainCompanyName").value("본사"));
    }

    @Test
    @DisplayName("별명 설정 성공")
    void updateAlias_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(1L, 1L, "본사", 2L, "하청", "SUB001", "ACCEPTED", null, "우리하청", null);
        given(partnershipService.updateAlias(eq(1L), eq(1L), eq("우리하청"))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/partnerships/1/alias")
                        .with(authentication(auth(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AliasRequest("우리하청"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alias").value("우리하청"));
    }

    @Test
    @DisplayName("파트너십 해제 성공")
    void removePartnership_success() throws Exception {
        // given
        willDoNothing().given(partnershipService).removePartnership(1L, 1L);

        // when & then
        mockMvc.perform(delete("/api/v1/partnerships/1")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("파트너십 해제 실패 - 미인증 401")
    void removePartnership_unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/partnerships/1"))
                .andExpect(status().isUnauthorized());
    }

    // ===================== 초대 수신함 =====================

    @Test
    @DisplayName("수신함 수락 성공")
    void acceptById_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(
                1L, 1L, "본사", 2L, "하청", "SUB001", "ACCEPTED", null, null, null);
        given(partnershipService.acceptById(2L, 1L)).willReturn(response);

        // when & then - 토큰 방식 POST /partnerships/accept와 경로가 겹치지 않는 것도 함께 확인된다
        mockMvc.perform(post("/api/v1/partnerships/1/accept")
                        .with(authentication(auth(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("받은 초대 목록 조회 성공")
    void getReceivedInvites_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(
                1L, 1L, "본사", 2L, "하청", "SUB001", "PENDING", null, null, null);
        given(partnershipService.getReceivedInvites(2L)).willReturn(List.of(response));

        // when & then - 하청 화면은 본사 이름을 본다
        mockMvc.perform(get("/api/v1/partnerships/invites/received")
                        .with(authentication(auth(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mainCompanyName").value("본사"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("보낸 초대 목록 조회 성공")
    void getSentInvites_success() throws Exception {
        // given
        PartnershipResponse response = new PartnershipResponse(
                1L, 1L, "본사", 2L, "하청", "SUB001", "PENDING", null, null, null);
        given(partnershipService.getSentInvites(1L)).willReturn(List.of(response));

        // when & then - 본사 화면은 하청 이름을 본다
        mockMvc.perform(get("/api/v1/partnerships/invites/sent")
                        .with(authentication(auth(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subCompanyName").value("하청"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
}
