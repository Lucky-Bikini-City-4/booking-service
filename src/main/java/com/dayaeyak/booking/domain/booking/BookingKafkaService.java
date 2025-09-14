package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.domain.booking.dto.kafka.BookingCancelRequestDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.BookingRequestKafkaDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookCancelDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookConfirmDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingKafkaService {
    private final KafkaTemplate<String, BookingRequestKafkaDto> kafkaTemplateBR; // Booking Request
    private final KafkaTemplate<String, BookingCancelRequestDto> kafkaTemplateBCR; // Booking Cancel Request
    private final KafkaTemplate<String, RestaurantBookConfirmDto> kafkaTemplateRBC; // Booking Confirm Request
    private final KafkaTemplate<String, RestaurantBookCancelDto> kafkaTemplateRBCa; // Booking Cancel

    public void sendBookingRequest (String topic, String key, BookingRequestKafkaDto dto) {
        kafkaTemplateBR.send(topic, key, dto);
    }

    public void sendBookingCancelRequest (String topic, String key, BookingCancelRequestDto dto) {
        kafkaTemplateBCR.send(topic, key, dto);
    }

    public void sendBookingRestaurantConfirm (String topic, String key, RestaurantBookConfirmDto dto) {
        kafkaTemplateRBC.send(topic, key, dto);
    }

    public void sendBookingRestaurantCancel (String topic, String key, RestaurantBookCancelDto dto) {
        kafkaTemplateRBCa.send(topic, key, dto);
    }
}
