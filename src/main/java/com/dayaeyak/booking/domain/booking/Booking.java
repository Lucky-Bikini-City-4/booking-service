package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.common.entuty.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.dayaeyak.booking.domain.booking.enums.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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



}
