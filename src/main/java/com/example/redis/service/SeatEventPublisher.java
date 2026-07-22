package com.example.redis.service;

import com.example.redis.dto.SeatEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SeatEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic seatEventsTopic;
    private final ObjectMapper objectMapper;

    public SeatEventPublisher(RedisTemplate<String, Object> redisTemplate,
                              ChannelTopic seatEventsTopic,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.seatEventsTopic = seatEventsTopic;
        this.objectMapper = objectMapper;
    }

    public void publish(Long seatId, Long flightId, String seatNumber, String eventType) {
        SeatEventMessage event = SeatEventMessage.builder()
                .seatId(seatId)
                .flightId(flightId)
                .seatNumber(seatNumber)
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(seatEventsTopic.getTopic(), json);
            log.info("📤 [Pub/Sub] Published event: {} for seat {} (seatId={})",
                    eventType, seatNumber, seatId);
        } catch (JsonProcessingException e) {
            log.error("❌ [Pub/Sub] Failed to serialize event: {}", e.getMessage(), e);
        }
    }
}
