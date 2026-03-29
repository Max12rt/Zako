package zako.monolith.train;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zako.monolith.exception.ResourceNotFoundException;
import zako.monolith.train.dto.TrainRequest;
import zako.monolith.train.dto.TrainResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainService {

    private final TrainRepository trainRepository;

    public TrainResponse create(TrainRequest request) {
        if (trainRepository.existsByTrainNumber(request.trainNumber())) {
            throw new IllegalStateException("Train number already exists: " + request.trainNumber());
        }
        Train train = Train.builder()
                .trainNumber(request.trainNumber())
                .type(request.type())
                .totalSeats(request.totalSeats())
                .build();
        return TrainResponse.from(trainRepository.save(train));
    }

    public TrainResponse getById(Long id) {
        return TrainResponse.from(findById(id));
    }

    public List<TrainResponse> getAll() {
        return trainRepository.findAll().stream().map(TrainResponse::from).toList();
    }

    public TrainResponse update(Long id, TrainRequest request) {
        Train train = findById(id);
        train.setTrainNumber(request.trainNumber());
        train.setType(request.type());
        train.setTotalSeats(request.totalSeats());
        return TrainResponse.from(trainRepository.save(train));
    }

    public void delete(Long id) {
        trainRepository.delete(findById(id));
    }

    public Train findById(Long id) {
        return trainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Train not found: " + id));
    }
}
