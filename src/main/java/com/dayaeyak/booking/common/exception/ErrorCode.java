// src/main/java/com/dayaeyak/booking/common/exception/ErrorCode.java
package com.dayaeyak.booking.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, " Method Not Allowed"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, " Entity Not Found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST,  "유효하지 않은 타입입니다."),

    // Booking
    BOOKING_NOT_FOUND(HttpStatus.NOT_FOUND,  "해당 예약을 찾을 수 없습니다."),
    DUPLICATE_BOOKING(HttpStatus.CONFLICT, "이미 예약된 시간입니다."),

    // Booking Detail
    BOOKING_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 예약 상세 정보를 찾을 수 없습니다."),
    DUPLICATE_BOOKING_DETAIL(HttpStatus.CONFLICT, "이미 예약 상세 정보가 존재합니다."),

    // Orchestration & External Services
    SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "이미 판매된 좌석입니다."),
    SEAT_ALREADY_LOCKED(HttpStatus.CONFLICT, "이미 다른 사용자가 선점한 좌석입니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "결제에 실패했습니다."),
    INSUFFICIENT_SEATS(HttpStatus.CONFLICT, "좌석이 부족합니다."),

    SEAT_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "좌석이 충분하지 않습니다."),

    CANCELLATION_PERIOD_EXPIRED(HttpStatus.NOT_FOUND, " 취소 기간이 만료되었습니다."),
    ALREADY_CANCELLED_BOOKING(HttpStatus.NOT_FOUND, " 이미 취소된 예약입니다."),

    //AuthorizationErrorCode
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "유효하지 않은 유저 ID입니다."),
    INVALID_USER_ROLE(HttpStatus.BAD_REQUEST, "유효하지 않은 유저 권한입니다."),
    REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "요청 접근 권한이 부족합니다.");


    private final HttpStatus status;
    private final String message;
}
