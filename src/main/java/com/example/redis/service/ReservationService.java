package com.example.redis.service;

import com.example.redis.dto.ReservationResponse;
import com.example.redis.entity.Reservation;
import com.example.redis.entity.Seat;
import com.example.redis.enums.ReservationStatus;
import com.example.redis.enums.SeatStatus;
import com.example.redis.exception.InvalidOperationException;
import com.example.redis.exception.ResourceNotFoundException;
import com.example.redis.repository.ReservationRepository;
import com.example.redis.repository.SeatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;
    private final SeatEventPublisher seatEventPublisher;

    public ReservationService(ReservationRepository reservationRepository,
                              SeatRepository seatRepository,
                              SeatLockService seatLockService,
                              SeatEventPublisher seatEventPublisher) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
        this.seatEventPublisher = seatEventPublisher;
    }

    @Transactional
    public ReservationResponse confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException(
                    "Cannot confirm reservation with status: " + reservation.getStatus());
        }

        // Update reservation status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // Update seat status to BOOKED (permanent)
        Seat seat = reservation.getSeat();
        seat.setStatus(SeatStatus.BOOKED);
        seatRepository.save(seat);

        // Release the Redis lock (no longer needed, seat is permanently sold)
        seatLockService.unlockSeat(seat.getId());

        // Publish event
        seatEventPublisher.publish(
                seat.getId(),
                seat.getFlight().getId(),
                seat.getSeatNumber(),
                "SEAT_BOOKED"
        );

        log.info("Reservation {} confirmed — seat {} is now BOOKED",
                reservationId, seat.getSeatNumber());

        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException(
                    "Cannot cancel reservation with status: " + reservation.getStatus());
        }

        // Update reservation status
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Release seat back to AVAILABLE
        Seat seat = reservation.getSeat();
        seat.setStatus(SeatStatus.AVAILABLE);
        seatRepository.save(seat);

        // Release the Redis lock
        seatLockService.unlockSeat(seat.getId());

        // Publish event
        seatEventPublisher.publish(
                seat.getId(),
                seat.getFlight().getId(),
                seat.getSeatNumber(),
                "SEAT_RELEASED"
        );

        log.info("Reservation {} cancelled — seat {} is now AVAILABLE",
                reservationId, seat.getSeatNumber());

        return toResponse(reservation);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .seatId(reservation.getSeat().getId())
                .seatNumber(reservation.getSeat().getSeatNumber())
                .passengerName(reservation.getPassengerName())
                .passengerEmail(reservation.getPassengerEmail())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
