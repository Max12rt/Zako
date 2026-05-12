package zako.ticket.ticket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TripDto(Long id, Integer availableSeats) {}
