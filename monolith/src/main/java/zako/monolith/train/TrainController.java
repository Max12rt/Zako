package zako.monolith.train;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zako.monolith.train.dto.TrainRequest;
import zako.monolith.train.dto.TrainResponse;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainResponse create(@Valid @RequestBody TrainRequest request) {
        return trainService.create(request);
    }

    @GetMapping("/{id}")
    public TrainResponse getById(@PathVariable Long id) {
        return trainService.getById(id);
    }

    @GetMapping
    public List<TrainResponse> getAll() {
        return trainService.getAll();
    }

    @PutMapping("/{id}")
    public TrainResponse update(@PathVariable Long id, @Valid @RequestBody TrainRequest request) {
        return trainService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        trainService.delete(id);
    }
}
