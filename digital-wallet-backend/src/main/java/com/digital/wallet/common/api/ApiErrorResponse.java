package com.digital.wallet.common.api;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ApiErrorResponse {
    private LocalDateTime timeStamp;
    private boolean success;
    private String error;
    private HttpStatus status;

    public ApiErrorResponse(String error, boolean success, HttpStatus status){
        this.timeStamp = LocalDateTime.now();
        this.success = success;
        this.error = error;
        this.status = status;
    }
}
