package zako.ticket.ticket.dto;
import java.math.BigDecimal;
public record TicketRequest(Long tripId, Long fromStationId, Long toStationId, BigDecimal price) {}
