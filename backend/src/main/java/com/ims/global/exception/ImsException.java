package com.ims.global.exception;

import lombok.Getter;

@Getter
public class ImsException extends RuntimeException {

    private final ErrorCode errorCode;

    public ImsException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
