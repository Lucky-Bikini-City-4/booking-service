package com.dayaeyak.booking.domain.booking;

import com.dayaeyak.booking.domain.booking.enums.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Book;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking,Long> {
    List<Booking> findByServiceTypeAndServiceId(ServiceType serviceType, Long serviceId);
}
