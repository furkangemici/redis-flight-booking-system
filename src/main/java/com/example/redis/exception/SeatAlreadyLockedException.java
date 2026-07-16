package com.example.redis.exception;

public class SeatAlreadyLockedException extends RuntimeException {

    public SeatAlreadyLockedException(Long seatId) {
        super("Seat " + seatId + " is currently being processed by another user");
    }

    public SeatAlreadyLockedException(String message) {
        super(message);
    }
}
