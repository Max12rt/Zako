package zako.trip.trip;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zako.trip.exception.ResourceNotFoundException;
import zako.trip.route.Route;
import zako.trip.route.RouteRepository;
import zako.trip.train.Train;
import zako.trip.train.TrainService;
import zako.trip.trip.dto.TripRequest;
import zako.trip.trip.dto.TripResponse;
import zako.trip.trip.dto.TripSearchRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TrainService trainService;
    private final RouteRepository routeRepository;

    @Transactional
    public TripResponse create(TripRequest request) {
        Train train = trainService.findById(request.trainId());
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new ResourceNotFoundException("Route not found: " + request.routeId()));

        Trip trip = Trip.builder()
                .train(train)
                .route(route)
                .departureTime(request.departureTime())
                .arrivalTime(request.arrivalTime())
                .availableSeats(train.getTotalSeats())
                .build();
        return TripResponse.from(tripRepository.save(trip));
    }

    public List<TripResponse> search(TripSearchRequest request) {
        LocalDateTime from = request.date().atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        return tripRepository.searchTrips(request.fromStationId(), request.toStationId(), from, to)
                .stream().map(TripResponse::from).toList();
    }

    public TripResponse getById(Long id) {
        return TripResponse.from(findById(id));
    }

    public List<TripResponse> getAll() {
        return tripRepository.findAll().stream().map(TripResponse::from).toList();
    }

    @Transactional
    public TripResponse updateStatus(Long id, TripStatus status) {
        Trip trip = findById(id);
        trip.setStatus(status);
        return TripResponse.from(tripRepository.save(trip));
    }

    public Trip findById(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + id));
    }
}
