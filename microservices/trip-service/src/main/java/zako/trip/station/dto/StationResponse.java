package zako.trip.station.dto;

import zako.trip.station.Station;

public record StationResponse(
        Long id,
        String name,
        String city,
        String code,
        Double latitude,
        Double longitude
) {
    public static StationResponse from(Station station) {
        return new StationResponse(
                station.getId(),
                station.getName(),
                station.getCity(),
                station.getCode(),
                station.getLatitude(),
                station.getLongitude()
        );
    }
}
