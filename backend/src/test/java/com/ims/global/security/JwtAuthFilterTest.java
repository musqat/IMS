package com.ims.global.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * JwtAuthFilter 단위 테스트
 * - 헤더에서 토큰을 꺼내 SecurityContext에 넣는 경로가 미검증 상태였다
 * - 토큰 없음 / 형식 오류 / 무효 / refresh 토큰일 때 인증이 설정되지 않아야 하고,
 *   그럼에도 요청은 다음 필터로 넘어가야 한다
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String TOKEN = "test-token";

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private FilterChain filterChain;

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void tearDown() {
        // SecurityContextHolder는 ThreadLocal이라 끝나고 정리한다
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWith(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("유효한 Access Token이면 SecurityContext에 userId가 담긴다")
    void validAccessToken_setsAuthentication() throws Exception {
        // given
        MockHttpServletRequest request = requestWith("Bearer " + TOKEN);
        given(jwtProvider.isValid(TOKEN)).willReturn(true);
        given(jwtProvider.isRefreshToken(TOKEN)).willReturn(false);
        given(jwtProvider.getUserId(TOKEN)).willReturn(1L);

        // when
        jwtAuthFilter.doFilter(request, response, filterChain);

        // then
        assertThat(currentAuthentication()).isNotNull();
        assertThat(currentAuthentication().getPrincipal()).isEqualTo(1L);
        assertThat(currentAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증이 설정되지 않는다")
    void noHeader_doesNotAuthenticate() throws Exception {
        // given
        MockHttpServletRequest request = requestWith(null);

        // when
        jwtAuthFilter.doFilter(request, response, filterChain);

        // then — 인증은 없지만 요청 자체를 막지는 않는다
        assertThat(currentAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtProvider).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 토큰을 추출하지 않는다")
    void malformedHeader_doesNotAuthenticate() throws Exception {
        // given — Bearer 없이 토큰만 보낸 경우
        MockHttpServletRequest request = requestWith(TOKEN);

        // when
        jwtAuthFilter.doFilter(request, response, filterChain);

        // then
        assertThat(currentAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
        then(jwtProvider).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증이 설정되지 않는다")
    void invalidToken_doesNotAuthenticate() throws Exception {
        // given
        MockHttpServletRequest request = requestWith("Bearer " + TOKEN);
        given(jwtProvider.isValid(TOKEN)).willReturn(false);

        // when
        jwtAuthFilter.doFilter(request, response, filterChain);

        // then — 현재 구현은 만료/위조 토큰을 조용히 무시하고 통과시킨다.
        // 최종 차단은 SecurityConfig의 authenticationEntryPoint가 401로 처리한다.
        assertThat(currentAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("Refresh Token으로는 인증되지 않는다")
    void refreshToken_doesNotAuthenticate() throws Exception {
        // given — 서명은 유효하지만 용도가 다른 토큰
        MockHttpServletRequest request = requestWith("Bearer " + TOKEN);
        given(jwtProvider.isValid(TOKEN)).willReturn(true);
        given(jwtProvider.isRefreshToken(TOKEN)).willReturn(true);

        // then — Refresh Token으로 일반 API를 호출하는 것을 막는 분기
        jwtAuthFilter.doFilter(request, response, filterChain);

        assertThat(currentAuthentication()).isNull();
        then(jwtProvider).should(never()).getUserId(anyString());
        then(filterChain).should().doFilter(request, response);
    }
}
