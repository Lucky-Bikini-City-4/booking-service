package com.dayaeyak.booking.config;

import com.dayaeyak.booking.domain.booking.dto.kafka.BookingCancelRequestDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.BookingRequestKafkaDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookCancelDto;
import com.dayaeyak.booking.domain.booking.dto.kafka.RestaurantBookConfirmDto;
import com.dayaeyak.booking.domain.booking.dto.request.BookingRequestDto;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final String SERVER = "localhost:9092";

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
}


