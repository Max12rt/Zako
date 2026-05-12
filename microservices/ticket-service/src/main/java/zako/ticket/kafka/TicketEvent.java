package zako.ticket.kafka;
public record TicketEvent(String type, Long ticketId, Long tripId, int seats) {}
