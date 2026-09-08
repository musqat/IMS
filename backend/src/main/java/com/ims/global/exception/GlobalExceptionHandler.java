package com.ims.global.exception;

import com.ims.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ImsException.class)
    public ResponseEntity<ApiResponse<Void>> handleImsException(ImsException e) {
        ErrorCode errorCode = e.getErrorCode();
        return new ResponseEntity<>(ApiResponse.fail(errorCode), errorCode.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        return new ResponseEntity<>(ApiResponse.fail(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e) {
        return new ResponseEntity<>(ApiResponse.fail(ErrorCode.UNAUTHORIZED), HttpStatus.UNAUTHORIZED);
    }

    /** 예외 메시지를 그대로 내보내면 클라이언트가 보낸 값이 응답에 실린다. 이름만 알린다 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(Exception e) {
        String name = e instanceof MissingServletRequestParameterException missing
                ? missing.getParameterName()
                : ((MethodArgumentTypeMismatchException) e).getName();
        return new ResponseEntity<>(
                ApiResponse.fail("요청 파라미터 '" + name + "'이(가) 올바르지 않습니다."),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return new ResponseEntity<>(ApiResponse.fail("요청 본문을 읽을 수 없습니다. JSON 형식을 확인하세요."), HttpStatus.BAD_REQUEST);
    }

    /** 중복 키와 외래 키 위반이 같은 예외로 올라온다. SQLState로 가른다 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        ErrorCode errorCode = switch (sqlState(e)) {
            case "23503" -> ErrorCode.REFERENCED_RESOURCE;
            case "23505" -> ErrorCode.DUPLICATE_RESOURCE;
            default -> ErrorCode.DUPLICATE_RESOURCE;
        };
        log.warn("제약 위반 SQLState={} -> {}", sqlState(e), errorCode.name());
        return new ResponseEntity<>(ApiResponse.fail(errorCode), HttpStatus.CONFLICT);
    }

    /** 가장 안쪽 SQLException의 SQLState. 없으면 빈 문자열 */
    private String sqlState(DataIntegrityViolationException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
            cause = cause.getCause();
        }
        return "";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        return new ResponseEntity<>(ApiResponse.fail(ErrorCode.FORBIDDEN), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(ApiResponse.fail(errorCode), errorCode.getStatus());
    }
}
