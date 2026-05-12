package zako.trip.station;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zako.trip.station.dto.StationRequest;
import zako.trip.station.dto.StationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse create(@Valid @RequestBody StationRequest request) {
        return stationService.create(request);
    }

    @GetMapping("/{id}")
    public StationResponse getById(@PathVariable Long id) {
        return stationService.getById(id);
    }

    @GetMapping
    public List<StationResponse> getAll() {
        return stationService.getAll();
    }

    @GetMapping("/search")
    public List<StationResponse> search(@RequestParam String query) {
        return stationService.search(query);
    }

    @GetMapping("/nearest")
    public StationResponse nearest(@RequestParam double lat, @RequestParam double lon) {
        return stationService.nearest(lat, lon);
    }

    @PutMapping("/{id}")
    public StationResponse update(@PathVariable Long id, @Valid @RequestBody StationRequest request) {
        return stationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        stationService.delete(id);
    }
}
