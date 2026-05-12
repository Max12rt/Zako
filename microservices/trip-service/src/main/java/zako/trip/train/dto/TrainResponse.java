package zako.trip.train.dto;

import zako.trip.train.Train;
import zako.trip.train.TrainType;

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
