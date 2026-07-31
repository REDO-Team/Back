package com.redo.domain.certification.exception;

import com.redo.domain.certification.controller.CertificationController;
import com.redo.global.apiPayload.ApiResponse;
import com.redo.global.apiPayload.code.BaseErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CertificationController.class)
public class CertificationExceptionHandler {

    @ExceptionHandler(CertificationException.class)
    public ResponseEntity<ApiResponse<?>> handleCertificationException(
            CertificationException exception
    ) {
        BaseErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.onFailure(errorCode, exception.getErrorDetail()));
    }
}
