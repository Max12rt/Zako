package zako.notification.kafka;

public record TicketEvent(String type, Long ticketId, Long userId, Long tripId, int seats,
                           String ticketCode, String fromCity, String toCity) {}
