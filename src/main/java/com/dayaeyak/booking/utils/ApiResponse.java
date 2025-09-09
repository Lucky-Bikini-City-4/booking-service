package com.dayaeyak.booking.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private String message;
    private T data;

    public static <T> ResponseEntity<ApiResponse<T>> success(int status, T data) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(null, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(int status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(int status, String message, T data) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(int status, String message){
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, null));
    }
}