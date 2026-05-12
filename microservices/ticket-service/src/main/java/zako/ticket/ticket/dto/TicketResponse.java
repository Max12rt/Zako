package zako.ticket.ticket.dto;
import zako.ticket.ticket.Ticket;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
    Long id, Long userId, Long tripId,
    StationInfo fromStation, StationInfo toStation,
    Integer seatNumber,
    LocalDateTime departureTime, LocalDateTime arrivalTime,
    BigDecimal price,
    String status, String paymentStatus,
    String ticketCode, LocalDateTime purchasedAt
) {
    public record StationInfo(Long id, String name, String city, String code) {}

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
            t.getId(), t.getUserId(), t.getTripId(),
            new StationInfo(t.getFromStationId(), t.getFromStationName(), t.getFromStationCity(), t.getFromStationCode()),
            new StationInfo(t.getToStationId(), t.getToStationName(), t.getToStationCity(), t.getToStationCode()),
            t.getSeatNumber(),
            t.getDepartureTime(), t.getArrivalTime(),
            t.getPrice(),
            t.getStatus().name(), t.getPaymentStatus().name(),
            t.getTicketCode(), t.getPurchasedAt()
        );
    }
}
