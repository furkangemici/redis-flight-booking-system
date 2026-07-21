package com.example.redis.service;

import com.example.redis.dto.ReservationResponse;
import com.example.redis.dto.SeatLockRequest;
import com.example.redis.entity.Reservation;
import com.example.redis.entity.Seat;
import com.example.redis.enums.ReservationStatus;
import com.example.redis.enums.SeatStatus;
import com.example.redis.exception.InvalidOperationException;
import com.example.redis.exception.ResourceNotFoundException;
import com.example.redis.exception.SeatAlreadyLockedException;
import com.example.redis.repository.ReservationRepository;
import com.example.redis.repository.SeatRepository;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SeatLockService {

    private static final long LOCK_WAIT_TIME_SECONDS = 3;
    private static final long LOCK_LEASE_TIME_SECONDS = 600; // 10 minutes
    private static final String LOCK_KEY_PREFIX = "seat-lock:";

    private final RedissonClient redissonClient;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final SeatEventPublisher seatEventPublisher;

    public SeatLockService(RedissonClient redissonClient,
                           SeatRepository seatRepository,
                           ReservationRepository reservationRepository,
                           SeatEventPublisher seatEventPublisher) {
        this.redissonClient = redissonClient;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.seatEventPublisher = seatEventPublisher;
    }

    @Transactional
    public ReservationResponse lockSeat(Long seatId, SeatLockRequest request) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", seatId));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new InvalidOperationException(
                    "Seat " + seat.getSeatNumber() + " is not available (current status: " + seat.getStatus() + ")");
        }

        String lockKey = LOCK_KEY_PREFIX + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean acquired;
        try {
            acquired = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition was interrupted", e);
        }

        if (!acquired) {
            log.warn("[Lock] Failed to acquire lock for seat {} (seatId={})",
                    seat.getSeatNumber(), seatId);
            throw new SeatAlreadyLockedException(seatId);
        }

        log.info("[Lock] Acquired distributed lock for seat {} (seatId={}, TTL={}s)",
                seat.getSeatNumber(), seatId, LOCK_LEASE_TIME_SECONDS);

        // Update seat status
        seat.setStatus(SeatStatus.LOCKED);
        seatRepository.save(seat);

        // Create PENDING reservation
        Reservation reservation = Reservation.builder()
                .seat(seat)
                .passengerName(request.getPassengerName())
                .passengerEmail(request.getPassengerEmail())
                .status(ReservationStatus.PENDING)
                .build();
        Reservation saved = reservationRepository.save(reservation);

        // Publish event
        seatEventPublisher.publish(
                seat.getId(),
                seat.getFlight().getId(),
                seat.getSeatNumber(),
                "SEAT_LOCKED"
        );

        LocalDateTime lockExpiresAt = LocalDateTime.now().plusSeconds(LOCK_LEASE_TIME_SECONDS);

        return ReservationResponse.builder()
                .id(saved.getId())
                .seatId(seat.getId())
                .seatNumber(seat.getSeatNumber())
                .passengerName(saved.getPassengerName())
                .passengerEmail(saved.getPassengerEmail())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .lockExpiresAt(lockExpiresAt)
                .build();
    }

    public void unlockSeat(Long seatId) {
        String lockKey = LOCK_KEY_PREFIX + seatId;
        RLock lock = redissonClient.getLock(lockKey);

        if (lock.isLocked()) {
            try {
                lock.forceUnlock();
                log.info("[Lock] Released distributed lock for seatId={}", seatId);
            } catch (Exception e) {
                log.warn("[Lock] Could not release lock for seatId={}: {}", seatId, e.getMessage());
            }
        }
    }
}
