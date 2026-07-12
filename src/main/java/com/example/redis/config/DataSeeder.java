package com.example.redis.config;

import com.example.redis.entity.Flight;
import com.example.redis.entity.Seat;
import com.example.redis.enums.SeatClass;
import com.example.redis.enums.SeatStatus;
import com.example.redis.repository.FlightRepository;
import com.example.redis.repository.SeatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DataSeeder implements CommandLineRunner {

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;

    public DataSeeder(FlightRepository flightRepository, SeatRepository seatRepository) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    public void run(String... args) {
        if (flightRepository.count() > 0) {
            log.info("Data already exists, skipping seeding.");
            return;
        }

        log.info("Seeding sample data...");

        // Create flights
        Flight flight1 = flightRepository.save(Flight.builder()
                .flightNumber("TK-101")
                .origin("IST")
                .destination("ANK")
                .departureTime(LocalDateTime.of(2026, 7, 5, 8, 0))
                .arrivalTime(LocalDateTime.of(2026, 7, 5, 9, 15))
                .basePrice(new BigDecimal("450.00"))
                .build());

        Flight flight2 = flightRepository.save(Flight.builder()
                .flightNumber("TK-202")
                .origin("IST")
                .destination("IZM")
                .departureTime(LocalDateTime.of(2026, 7, 5, 10, 30))
                .arrivalTime(LocalDateTime.of(2026, 7, 5, 11, 45))
                .basePrice(new BigDecimal("380.00"))
                .build());

        Flight flight3 = flightRepository.save(Flight.builder()
                .flightNumber("TK-303")
                .origin("ANK")
                .destination("IZM")
                .departureTime(LocalDateTime.of(2026, 7, 5, 14, 0))
                .arrivalTime(LocalDateTime.of(2026, 7, 5, 15, 30))
                .basePrice(new BigDecimal("520.00"))
                .build());

        // Create seats for each flight
        for (Flight flight : List.of(flight1, flight2, flight3)) {
            createSeatsForFlight(flight);
        }

        log.info("Sample data seeded: 3 flights, 18 seats total.");
    }

    private void createSeatsForFlight(Flight flight) {
        // 4 Economy seats
        String[] economySeats = {"12A", "12B", "12C", "12D"};
        for (String seatNum : economySeats) {
            seatRepository.save(Seat.builder()
                    .flight(flight)
                    .seatNumber(seatNum)
                    .seatClass(SeatClass.ECONOMY)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        // 2 Business seats
        String[] businessSeats = {"1A", "1B"};
        for (String seatNum : businessSeats) {
            seatRepository.save(Seat.builder()
                    .flight(flight)
                    .seatNumber(seatNum)
                    .seatClass(SeatClass.BUSINESS)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }
    }
}
