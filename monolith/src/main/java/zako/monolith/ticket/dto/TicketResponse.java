package zako.monolith.ticket.dto;

import zako.monolith.station.dto.StationResponse;
import zako.monolith.ticket.Ticket;
import zako.monolith.ticket.PaymentStatus;
import zako.monolith.ticket.TicketStatus;
import zako.monolith.trip.dto.TripResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        Long userId,
        TripResponse trip,
        StationResponse fromStation,
        StationResponse toStation,
        Integer seatNumber,
        BigDecimal price,
        TicketStatus status,
        PaymentStatus paymentStatus,
        String ticketCode,
        LocalDateTime purchasedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getUser().getId(),
                TripResponse.from(ticket.getTrip()),
                StationResponse.from(ticket.getFromStation()),
                StationResponse.from(ticket.getToStation()),
                ticket.getSeatNumber(),
                ticket.getPrice(),
                ticket.getStatus(),
                ticket.getPaymentStatus(),
                ticket.getTicketCode(),
                ticket.getPurchasedAt()
        );
    }
}
