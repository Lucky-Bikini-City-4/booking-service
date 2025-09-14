package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.domain.booking.dto.kafka.BookingCancelRequestDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.BookingRequestKafkaDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookCancelDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookConfirmDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/kafka")
public class BookingKafkaController {
    private final BookingKafkaService bookingKafkaService;

    @PostMapping("/1")
    public String sendMessageQueue(@RequestParam("topic") String topic,
                                   @RequestParam("key") String key,
                                   @RequestBody BookingRequestKafkaDto dto) {
        bookingKafkaService.sendBookingRequest(topic, key, dto);
        return "Message sent to Kafka topic" ;
    }

    @PostMapping("/2")
    public String sendMessageQueue2(@RequestParam("topic") String topic,
                                   @RequestParam("key") String key,
                                   @RequestBody BookingCancelRequestDto dto) {
        bookingKafkaService.sendBookingCancelRequest(topic, key, dto);
        return "Message sent to Kafka topic" ;
    }

    @PostMapping("/3")
    public String sendMessageQueue3(@RequestParam("topic") String topic,
                                   @RequestParam("key") String key,
                                   @RequestBody RestaurantBookConfirmDto dto) {
        bookingKafkaService.sendBookingRestaurantConfirm(topic, key, dto);
        return "Message sent to Kafka topic" ;
    }

    @PostMapping("/4")
    public String sendMessageQueue4(@RequestParam("topic") String topic,
                                   @RequestParam("key") String key,
                                   @RequestBody RestaurantBookCancelDto dto) {
        bookingKafkaService.sendBookingRestaurantCancel(topic, key, dto);
        return "Message sent to Kafka topic" ;
    }

}
