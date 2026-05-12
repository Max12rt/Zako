package zako.ticket.kafka;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import zako.ticket.ticket.Ticket;

@Component @RequiredArgsConstructor
public class TicketEventProducer {
    private static final String TOPIC_PURCHASED = "ticket.purchased";
    private static final String TOPIC_CANCELLED  = "ticket.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPurchased(Ticket t) {
        kafkaTemplate.send(TOPIC_PURCHASED,
            new TicketEvent(TOPIC_PURCHASED, t.getId(), t.getTripId(), 1));
    }
    public void publishCancelled(Ticket t) {
        kafkaTemplate.send(TOPIC_CANCELLED,
            new TicketEvent(TOPIC_CANCELLED, t.getId(), t.getTripId(), 1));
    }
}
