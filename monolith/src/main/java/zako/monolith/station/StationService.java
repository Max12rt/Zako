package zako.monolith.station;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import zako.monolith.exception.ResourceNotFoundException;
import zako.monolith.station.dto.StationRequest;
import zako.monolith.station.dto.StationResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;

    public StationResponse create(StationRequest request) {
        Station station = Station.builder()
                .name(request.name())
                .city(request.city())
                .code(request.code().toUpperCase())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
        return StationResponse.from(stationRepository.save(station));
    }

    public StationResponse getById(Long id) {
        return StationResponse.from(findById(id));
    }

    public List<StationResponse> getAll() {
        return stationRepository.findAll().stream().map(StationResponse::from).toList();
    }

    public List<StationResponse> search(String query) {
        return stationRepository.findByNameContainingIgnoreCase(query).stream()
                .map(StationResponse::from).toList();
    }

    public StationResponse update(Long id, StationRequest request) {
        Station station = findById(id);
        station.setName(request.name());
        station.setCity(request.city());
        station.setCode(request.code().toUpperCase());
        station.setLatitude(request.latitude());
        station.setLongitude(request.longitude());
        return StationResponse.from(stationRepository.save(station));
    }

    public void delete(Long id) {
        stationRepository.delete(findById(id));
    }

    public Station findById(Long id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + id));
    }
}
