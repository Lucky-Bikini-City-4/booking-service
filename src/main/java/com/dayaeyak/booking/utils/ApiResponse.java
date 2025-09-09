package com.dayaeyak.booking.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private HttpStatus httpStatus;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(HttpStatus status,T data) {
        return new ApiResponse<>(status,null,data);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message) {
        return new ApiResponse<>(status,message,null);
    }

    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status,message,data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message){
        return new ApiResponse<>(status, message, null);
    }
}