package com.example.redis.service;

import com.example.redis.dto.FlightCreateRequest;
import com.example.redis.dto.FlightResponse;
import com.example.redis.entity.Flight;
import com.example.redis.exception.ResourceNotFoundException;
import com.example.redis.repository.FlightRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Cacheable(value = "flightSearch", key = "#origin + '-' + #destination + '-' + #date")
    @Transactional(readOnly = true)
    public List<FlightResponse> searchFlights(String origin, String destination, LocalDate date) {
        log.info("[Cache MISS] Querying database for flights: {} -> {} on {}",
                origin, destination, date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Flight> flights = flightRepository
                .findByOriginAndDestinationAndDepartureTimeBetween(origin, destination, startOfDay, endOfDay);

        return flights.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, List<String>> getLocations() {
        List<String> origins = flightRepository.findDistinctOrigins();
        List<String> destinations = flightRepository.findDistinctDestinations();
        return java.util.Map.of("origins", origins, "destinations", destinations);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableDates(String origin, String destination) {
        return flightRepository.findDepartureTimesByOriginAndDestination(origin, destination)
                .stream()
                .map(java.time.LocalDateTime::toLocalDate)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", id));
        return toResponse(flight);
    }

    @CacheEvict(value = "flightSearch", allEntries = true)
    @Transactional
    public FlightResponse createFlight(FlightCreateRequest request) {
        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .basePrice(request.getBasePrice())
                .build();

        Flight saved = flightRepository.save(flight);
        log.info("Created flight: {} ({} -> {})",
                saved.getFlightNumber(), saved.getOrigin(), saved.getDestination());

        return toResponse(saved);
    }

    private FlightResponse toResponse(Flight flight) {
        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .basePrice(flight.getBasePrice())
                .build();
    }
}
