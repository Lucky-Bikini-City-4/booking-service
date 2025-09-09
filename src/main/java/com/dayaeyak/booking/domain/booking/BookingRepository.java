package com.dayaeyak.booking.domain.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Book;

public interface BookingRepository extends JpaRepository<Booking,Long> {
}
