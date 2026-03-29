package zako.monolith.trip.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TripRequest(
        @NotNull Long trainId,
        @NotNull Long routeId,
        @NotNull LocalDateTime departureTime,
        @NotNull LocalDateTime arrivalTime
) {}
