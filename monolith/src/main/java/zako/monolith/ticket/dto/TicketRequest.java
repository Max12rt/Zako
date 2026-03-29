package zako.monolith.ticket.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TicketRequest(
        @NotNull Long tripId,
        @NotNull Long fromStationId,
        @NotNull Long toStationId,
        @NotNull BigDecimal price
) {}
