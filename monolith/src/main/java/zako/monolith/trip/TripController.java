package zako.monolith.trip;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zako.monolith.trip.dto.TripRequest;
import zako.monolith.trip.dto.TripResponse;
import zako.monolith.trip.dto.TripSearchRequest;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse create(@Valid @RequestBody TripRequest request) {
        return tripService.create(request);
    }

    @PostMapping("/search")
    public List<TripResponse> search(@Valid @RequestBody TripSearchRequest request) {
        return tripService.search(request);
    }

    @GetMapping("/{id}")
    public TripResponse getById(@PathVariable Long id) {
        return tripService.getById(id);
    }

    @GetMapping
    public List<TripResponse> getAll() {
        return tripService.getAll();
    }

    @PatchMapping("/{id}/status")
    public TripResponse updateStatus(@PathVariable Long id, @RequestParam TripStatus status) {
        return tripService.updateStatus(id, status);
    }
}
