package zako.monolith.train.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import zako.monolith.train.TrainType;

public record TrainRequest(
        @NotBlank String trainNumber,
        @NotNull TrainType type,
        @NotNull @Min(1) Integer totalSeats
) {}
