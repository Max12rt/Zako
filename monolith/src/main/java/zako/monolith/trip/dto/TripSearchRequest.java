package zako.monolith.trip.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TripSearchRequest(
        @NotNull Long fromStationId,
        @NotNull Long toStationId,
        @NotNull LocalDate date
) {}
