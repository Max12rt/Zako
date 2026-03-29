package zako.monolith.ticket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zako.monolith.exception.ResourceNotFoundException;
import zako.monolith.station.Station;
import zako.monolith.station.StationService;
import zako.monolith.ticket.dto.TicketRequest;
import zako.monolith.ticket.dto.TicketResponse;
import zako.monolith.trip.Trip;
import zako.monolith.trip.TripService;
import zako.monolith.user.User;
import zako.monolith.user.UserService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserService userService;
    private final TripService tripService;
    private final StationService stationService;

    @Transactional
    public TicketResponse purchase(Long userId, TicketRequest request) {
        User user = userService.findById(userId);
        Trip trip = tripService.findById(request.tripId());
        Station fromStation = stationService.findById(request.fromStationId());
        Station toStation = stationService.findById(request.toStationId());

        if (trip.getAvailableSeats() <= 0) {
            throw new IllegalStateException("No available seats on this trip");
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);

        Ticket ticket = Ticket.builder()
                .user(user)
                .trip(trip)
                .fromStation(fromStation)
                .toStation(toStation)
                .price(request.price())
                .ticketCode(UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12))
                .build();

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse cancel(Long ticketId, Long userId) {
        Ticket ticket = findById(ticketId);

        if (!ticket.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Not authorized to cancel this ticket");
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Only active tickets can be cancelled");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.getTrip().setAvailableSeats(ticket.getTrip().getAvailableSeats() + 1);

        return TicketResponse.from(ticketRepository.save(ticket));
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
