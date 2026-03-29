package zako.monolith.train.dto;

import zako.monolith.train.Train;
import zako.monolith.train.TrainType;

public record TrainResponse(
        Long id,
        String trainNumber,
        TrainType type,
        Integer totalSeats
) {
    public static TrainResponse from(Train train) {
        return new TrainResponse(
                train.getId(),
                train.getTrainNumber(),
                train.getType(),
                train.getTotalSeats()
        );
    }
}
