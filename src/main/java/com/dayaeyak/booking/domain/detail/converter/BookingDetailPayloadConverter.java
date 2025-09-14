package com.dayaeyak.booking.domain.detail.converter;

import com.dayaeyak.booking.domain.detail.payload.BookingDetailPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BookingDetailPayloadConverter implements AttributeConverter<BookingDetailPayload, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(BookingDetailPayload attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting BookingDetailPayload to JSON", e);
        }
    }

    @Override
    public BookingDetailPayload convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, BookingDetailPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to BookingDetailPayload", e);
        }
    }
}