package com.example.redis.dto;

import com.example.redis.enums.ReservationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long id;
    private Long seatId;
    private String seatNumber;
    private String passengerName;
    private String passengerEmail;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lockExpiresAt;
}
