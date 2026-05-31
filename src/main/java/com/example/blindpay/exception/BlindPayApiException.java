package com.example.blindpay.exception;

import lombok.Getter;

@Getter
public class BlindPayApiException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public BlindPayApiException(int statusCode, String responseBody) {
        super("BlindPay API error: HTTP " + statusCode + " — " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
