package com.example.redis.repository;

import com.example.redis.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByOriginAndDestinationAndDepartureTimeBetween(
            String origin,
            String destination,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT f.origin FROM Flight f")
    List<String> findDistinctOrigins();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT f.destination FROM Flight f")
    List<String> findDistinctDestinations();

    @org.springframework.data.jpa.repository.Query("SELECT f.departureTime FROM Flight f WHERE f.origin = :origin AND f.destination = :destination")
    List<java.time.LocalDateTime> findDepartureTimesByOriginAndDestination(
            @org.springframework.data.repository.query.Param("origin") String origin,
            @org.springframework.data.repository.query.Param("destination") String destination
    );
}
