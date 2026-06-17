package zako.notification.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import zako.notification.notification.NotificationMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventConsumer {

    private final SimpMessagingTemplate messaging;

    @KafkaListener(topics = {"ticket.purchased", "ticket.cancelled"})
    public void onTicketEvent(TicketEvent event) {
        if (event.userId() == null) return;
        String userId = event.userId().toString();
        boolean purchased = "ticket.purchased".equals(event.type());
        String msg = purchased
            ? "Квиток %s → %s куплено!".formatted(event.fromCity(), event.toCity())
            : "Квиток %s → %s скасовано.".formatted(event.fromCity(), event.toCity());

        messaging.convertAndSendToUser(userId, "/queue/notifications",
            new NotificationMessage(purchased ? "purchased" : "cancelled", msg, event.ticketCode()));
        log.info("Sent {} notification to user {}", event.type(), userId);
    }
}
