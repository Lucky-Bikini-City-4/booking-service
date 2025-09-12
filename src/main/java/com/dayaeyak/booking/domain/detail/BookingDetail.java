package com.dayaeyak.booking.domain.detail;

import com.dayaeyak.booking.common.entuty.BaseEntity;
import com.dayaeyak.booking.domain.detail.dto.request.BookingDetailCreateRequestDto;
import com.dayaeyak.booking.domain.detail.dto.request.BookingDetailUpdateRequestDto;
import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import org.hibernate.annotations.Type; // Import Type
import com.vladmihalcea.hibernate.type.json.JsonType; // Import JsonType

@Getter
@Setter
@Entity
@Table(name = "booking_details")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingDetail extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Type(JsonType.class)
    @Column(name = "details", columnDefinition = "json")
    private BookingDetailPayload details;

    public void update(BookingDetailUpdateRequestDto requestDto) {
        if (requestDto.bookingId() != null) {
            this.bookingId = requestDto.bookingId();
        }
        if (requestDto.details() != null) {
            this.details = requestDto.details();
        }
        this.updatedAt = LocalDateTime.now();
    }
}