package com.example.redis.dto;

import com.example.redis.enums.SeatClass;
import com.example.redis.enums.SeatStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatResponse {

    private Long id;
    private Long flightId;
    private String seatNumber;
    private SeatClass seatClass;
    private SeatStatus status;
}
