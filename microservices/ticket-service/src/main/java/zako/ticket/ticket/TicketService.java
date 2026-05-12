package zako.ticket.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zako.ticket.client.TripServiceClient;
import zako.ticket.exception.ResourceNotFoundException;
import zako.ticket.kafka.TicketEventProducer;
import zako.ticket.ticket.dto.StationDto;
import zako.ticket.ticket.dto.TicketRequest;
import zako.ticket.ticket.dto.TicketResponse;
import zako.ticket.ticket.dto.TripDto;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TripServiceClient tripServiceClient;
    private final TicketEventProducer eventProducer;

    @Transactional
    public TicketResponse purchase(Long userId, TicketRequest request) {
        TripDto trip = tripServiceClient.getTrip(request.tripId());
        if (trip.availableSeats() == null || trip.availableSeats() <= 0) {
            throw new IllegalStateException("No available seats on this trip");
        }
        StationDto fromStation = tripServiceClient.getStation(request.fromStationId());
        StationDto toStation = tripServiceClient.getStation(request.toStationId());

        Ticket ticket = Ticket.builder()
                .userId(userId)
                .tripId(trip.id())
                .fromStationId(fromStation.id())
                .fromStationName(fromStation.name())
                .fromStationCity(fromStation.city())
                .fromStationCode(fromStation.code())
                .toStationId(toStation.id())
                .toStationName(toStation.name())
                .toStationCity(toStation.city())
                .toStationCode(toStation.code())
                .price(request.price())
                .ticketCode(UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12))
                .build();

        Ticket saved = ticketRepository.save(ticket);
        eventProducer.publishPurchased(saved);
        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketResponse pay(Long ticketId, Long userId) {
        Ticket ticket = findById(ticketId);
        if (!ticket.getUserId().equals(userId)) {
            throw new IllegalStateException("Not authorized to pay for this ticket");
        }
        if (ticket.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Ticket already paid");
        }
        ticket.setPaymentStatus(PaymentStatus.PAID);
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse cancel(Long ticketId, Long userId) {
        Ticket ticket = findById(ticketId);
        if (!ticket.getUserId().equals(userId)) {
            throw new IllegalStateException("Not authorized to cancel this ticket");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Only active tickets can be cancelled");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);
        eventProducer.publishCancelled(saved);
        return TicketResponse.from(saved);
    }

    public List<TicketResponse> getUserTickets(Long userId) {
        return ticketRepository.findByUserId(userId).stream().map(TicketResponse::from).toList();
    }

    public TicketResponse getById(Long id) {
        return TicketResponse.from(findById(id));
    }

    public Ticket findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }
}
