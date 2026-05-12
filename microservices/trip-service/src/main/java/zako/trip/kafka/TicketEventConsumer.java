package zako.trip.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import zako.trip.trip.TripRepository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventConsumer {

    private static final String TOPIC_PURCHASED = "ticket.purchased";
    private static final String TOPIC_CANCELLED  = "ticket.cancelled";

    private final TripRepository tripRepository;

    @KafkaListener(topics = {TOPIC_PURCHASED, TOPIC_CANCELLED})
    @Transactional
    public void onTicketEvent(TicketEvent event) {
        tripRepository.findById(event.tripId()).ifPresentOrElse(trip -> {
            int delta = TOPIC_CANCELLED.equals(event.type()) ? event.seats() : -event.seats();
            trip.setAvailableSeats(Math.max(0, trip.getAvailableSeats() + delta));
            tripRepository.save(trip);
            log.info("Updated availableSeats for trip {} by {}", event.tripId(), delta);
        }, () -> log.warn("Trip {} not found for event {}", event.tripId(), event.type()));
    }
}
