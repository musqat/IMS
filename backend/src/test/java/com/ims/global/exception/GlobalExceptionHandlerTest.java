package com.ims.global.exception;

import com.ims.global.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * GlobalExceptionHandler 단위 테스트
 * - 핸들러를 직접 호출해 어떤 상태 코드·코드·메시지를 내는지 체크한다
 * - code는 ErrorCode 이름이다. 프런트가 이걸로 분기하므로 메시지와 별개로 검증한다
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** MethodParameter 생성을 위한 더미 시그니처 */
    @SuppressWarnings("unused")
    private void dummyMethod(String param) {
    }

    private MethodParameter dummyParameter() throws NoSuchMethodException {
        return new MethodParameter(
                getClass().getDeclaredMethod("dummyMethod", String.class), 0);
    }

    @Test
    @DisplayName("ImsException은 ErrorCode의 상태와 메시지를 그대로 사용한다")
    void handleImsException_usesErrorCodeStatusAndMessage() {
        // when
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleImsException(new ImsException(ErrorCode.WAREHOUSE_NOT_OWNED));

        // then
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.WAREHOUSE_NOT_OWNED.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo(ErrorCode.WAREHOUSE_NOT_OWNED.getMessage());
        // 프런트는 이 값으로 분기한다. 메시지 문구가 바뀌어도 여기는 그대로여야 한다
        assertThat(response.getBody().code()).isEqualTo("WAREHOUSE_NOT_OWNED");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @DisplayName("검증 실패는 첫 필드 에러를 '필드: 메시지' 형식으로 반환한다")
    void handleValidationException_returnsFirstFieldError() throws Exception {
        // given
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "이메일은 필수입니다."));
        bindingResult.addError(new FieldError("request", "password", "비밀번호는 필수입니다."));

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(
                new MethodArgumentNotValidException(dummyParameter(), bindingResult));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("email: 이메일은 필수입니다.");
        // ErrorCode가 없는 실패다. 분기할 종류가 아니라 메시지를 그대로 보여주면 된다
        assertThat(response.getBody().code()).isNull();
    }

    @Test
    @DisplayName("필드 에러가 없으면 기본 메시지를 반환한다")
    void handleValidationException_noFieldError_returnsDefault() throws Exception {
        // given
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(
                new MethodArgumentNotValidException(dummyParameter(), bindingResult));

        // then
        assertThat(response.getBody().message()).isEqualTo("입력값이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("필수 헤더 누락은 401을 반환한다")
    void handleMissingHeader_returns401() throws Exception {
        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingHeader(
                new MissingRequestHeaderException("Authorization", dummyParameter()));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("필수 파라미터 누락은 400과 파라미터 이름을 반환한다")
    void handleMissingParam_returns400() {
        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParam(
                new MissingServletRequestParameterException("warehouseId", "Long"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("warehouseId");
    }

    @Test
    @DisplayName("타입이 안 맞는 파라미터는 400을 반환하고 보낸 값을 응답에 싣지 않는다")
    void handleTypeMismatch_doesNotExposeValue() {
        // given
        MethodArgumentTypeMismatchException e = new MethodArgumentTypeMismatchException(
                "notanumber", Long.class, "warehouseId", null,
                new NumberFormatException("For input string: \"notanumber\""));

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParam(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("warehouseId");
        assertThat(response.getBody().message()).doesNotContain("notanumber");
    }

    @Test
    @DisplayName("본문 파싱 실패는 400과 안내 메시지를 반환한다")
    void handleNotReadable_returns400() {
        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleNotReadable(
                new HttpMessageNotReadableException(
                        "malformed",
                        new MockHttpInputMessage("{".getBytes(StandardCharsets.UTF_8))));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("JSON 형식");
    }

    @Test
    @DisplayName("중복 키 위반은 DUPLICATE_RESOURCE로 나간다")
    void handleDataIntegrityViolation_uniqueViolation() {
        // given - SQLState 23505 = unique_violation
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "duplicate key", new SQLException("duplicate key", "23505"));

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE.name());
    }

    /** 안 가르면 삭제 요청에 "이미 존재하는 리소스입니다"가 나간다 */
    @Test
    @DisplayName("외래 키 위반은 REFERENCED_RESOURCE로 나간다")
    void handleDataIntegrityViolation_foreignKeyViolation() {
        // given - SQLState 23503 = foreign_key_violation
        DataIntegrityViolationException e = new DataIntegrityViolationException(
                "update or delete violates foreign key constraint",
                new SQLException("violates foreign key constraint", "23503"));

        // when
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(e);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REFERENCED_RESOURCE.name());
        assertThat(response.getBody().message()).isNotEqualTo(ErrorCode.DUPLICATE_RESOURCE.getMessage());
    }

    @Test
    @DisplayName("SQLState를 못 읽으면 기존 동작을 유지한다")
    void handleDataIntegrityViolation_noSqlState() {
        // given - 원인 예외가 SQLException이 아닌 경우
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("무결성 위반"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.DUPLICATE_RESOURCE.name());
    }

    @Test
    @DisplayName("인가 실패는 403을 반환한다")
    void handleAccessDenied_returns403() {
        // when
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500으로 변환한다")
    void handleException_returns500() {
        // when
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleException(new RuntimeException("boom"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        // 내부 예외 메시지("boom")가 응답에 새어나가지 않아야 한다
        assertThat(response.getBody().message()).doesNotContain("boom");
    }

    @Test
    @DisplayName("같은 409라도 code로 구분된다 - status만으로는 원인을 알 수 없다")
    void sameStatus_differentCode() {
        // given - 창고 삭제가 막히는 두 가지 원인. 둘 다 409다
        var byInventory = handler.handleImsException(
                new ImsException(ErrorCode.WAREHOUSE_HAS_INVENTORY));
        var byProduction = handler.handleImsException(
                new ImsException(ErrorCode.WAREHOUSE_HAS_PRODUCTION));

        // when & then - status는 같고 code는 다르다
        assertThat(byInventory.getStatusCode()).isEqualTo(byProduction.getStatusCode());
        assertThat(byInventory.getBody().code()).isEqualTo("WAREHOUSE_HAS_INVENTORY");
        assertThat(byProduction.getBody().code()).isEqualTo("WAREHOUSE_HAS_PRODUCTION");
    }

    @Test
    @DisplayName("성공 응답에는 code가 없다")
    void success_hasNoCode() {
        // when
        ApiResponse<String> response = ApiResponse.success("ok");

        // then
        assertThat(response.code()).isNull();
        assertThat(response.message()).isEqualTo("success");
        assertThat(response.data()).isEqualTo("ok");
    }
}
