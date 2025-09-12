package com.dayaeyak.booking.domain.booking.dto.request;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;

public record BookingFindByServiceDto(
        long serviceId,
        ServiceType serviceType
){

}
