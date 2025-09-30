package com.dayaeyak.booking.config;
import com.dayaeyak.booking.domain.booking.dto.kafka.BookingCancelRequestDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.BookingRequestKafkaDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookCancelDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookConfirmDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingPerformanceRequestDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingRequestDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    //private final String SERVER = "localhost:9092";
    private final String SERVER = "13.209.66.160:9092";

    @Bean
    public Map<String, Object> getStringObjectMap() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, SERVER);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    @Bean
    public Map<String, Object> producerConfigs() {
        return getStringObjectMap();

    }

    @Bean
    public ProducerFactory<String, BookingRequestKafkaDto> ProducerFactoryBR() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, BookingRequestKafkaDto> kafkaTemplateBR() {
        return new KafkaTemplate<>(ProducerFactoryBR());
    }

    @Bean
    public ProducerFactory<String, BookingCancelRequestDto> ProducerFactoryBC() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, BookingCancelRequestDto> kafkaTemplateBC() {
        return new KafkaTemplate<>(ProducerFactoryBC());
    }

    @Bean
    public ProducerFactory<String, RestaurantBookConfirmDto> ProducerFactoryRBC() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, RestaurantBookConfirmDto> kafkaTemplateRBC() {
        return new KafkaTemplate<>(ProducerFactoryRBC());
    }

    @Bean
    public ProducerFactory<String, RestaurantBookCancelDto> ProducerFactorRBCa() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, RestaurantBookCancelDto> kafkaTemplateRBCa() {
        return new KafkaTemplate<>(ProducerFactorRBCa());
    }

    // BookingPerformanceRequestDto를 위한 ProducerFactory
    @Bean
    public ProducerFactory<String, BookingRequestDto> ProducerFactoryBPR() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    // BookingPerformanceRequestDto를 위한 KafkaTemplate
    @Bean
    public KafkaTemplate<String, BookingRequestDto> kafkaTemplateBPR() {
        return new KafkaTemplate<>(ProducerFactoryBPR());
    }

    @Bean
    public ProducerFactory<String, Object> genericProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(genericProducerFactory());
    }


}


