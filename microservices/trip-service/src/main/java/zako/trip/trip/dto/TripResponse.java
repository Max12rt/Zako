package zako.trip.trip.dto;

import zako.trip.train.dto.TrainResponse;
import zako.trip.trip.Trip;
import zako.trip.trip.TripStatus;

import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        TrainResponse train,
        Long routeId,
        String routeName,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        TripStatus status,
        Integer availableSeats
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                TrainResponse.from(trip.getTrain()),
                trip.getRoute().getId(),
                trip.getRoute().getName(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                trip.getStatus(),
                trip.getAvailableSeats()
        );
    }
}
