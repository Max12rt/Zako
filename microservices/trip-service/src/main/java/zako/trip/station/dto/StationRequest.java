package zako.trip.station.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StationRequest(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank @Size(min = 2, max = 5) String code,
        Double latitude,
        Double longitude
) {}
