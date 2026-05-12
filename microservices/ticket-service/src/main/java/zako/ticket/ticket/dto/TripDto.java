package zako.ticket.ticket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TripDto(Long id, Integer availableSeats, LocalDateTime departureTime, LocalDateTime arrivalTime) {}
