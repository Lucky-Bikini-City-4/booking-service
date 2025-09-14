package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.common.entuty.BaseEntity;
import com.dayaeyak.booking.domain.booking.dto.request.BookingCreateRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingPerformanceRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingUpdateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.dayaeyak.booking.domain.booking.enums.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(name = "bookings")
@Entity
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType; // EXHIBITION, PERFORMANCE, RESTAURANT

    @Column(name = "total_fee")
    private Integer totalFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status; // PENDING, COMPLETED, CANCELLED, FAILED

    @Column(name = "cancel_deadline", nullable = false)
    private LocalDateTime cancelDeadline;

    public Booking(BookingCreateRequestDto requestDto) {
        this.userId = requestDto.userId();
        this.serviceId = requestDto.serviceId();
        this.serviceType = requestDto.serviceType();
        this.totalFee = requestDto.totalFee();
        this.status = requestDto.status();
        this.cancelDeadline = LocalDateTime.now().plusDays(3);;
    }

//    public Booking(BookingPerformanceRequestDto requestDto) {
//        this.userId = requestDto.userId();
//        this.serviceId = requestDto.serviceId();
//        this.serviceType = requestDto.serviceType();
//        this.totalFee = requestDto.totalFee();
//        this.status = requestDto.status();
//        this.cancelDeadline = LocalDateTime.now().plusDays(3);;
//    }

    public void update(BookingUpdateRequestDto requestDto) {
        if (requestDto.userId() != null) {
            this.userId = requestDto.userId();
        }
        if (requestDto.serviceId() != null) {
            this.serviceId = requestDto.serviceId();
        }
        if (requestDto.serviceType() != null) {
            this.serviceType = requestDto.serviceType();
        }
        if (requestDto.totalFee() != null) {
            this.totalFee = requestDto.totalFee();
        }
        if (requestDto.status() != null) {
            this.status = requestDto.status();
        }
        if (requestDto.cancelDeadline() != null) {
            this.cancelDeadline = requestDto.cancelDeadline();
        }
        this.updatedAt = LocalDateTime.now();


    }



}
