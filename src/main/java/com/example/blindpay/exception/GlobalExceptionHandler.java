package com.example.blindpay.exception;

import com.example.blindpay.dto.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BlindPayApiException.class)
    public ResponseEntity<ApiErrorResponse> handleBlindPayApiException(BlindPayApiException ex) {
        log.error("BlindPay API error: {}", ex.getMessage());
        ApiErrorResponse error = new ApiErrorResponse(
                ex.getStatusCode(),
                "BlindPay API Error",
                ex.getResponseBody()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Bad request: {}", ex.getMessage());
        ApiErrorResponse error = new ApiErrorResponse(400, "Bad Request", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        ApiErrorResponse error = new ApiErrorResponse(500, "Internal Server Error", ex.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }
}
