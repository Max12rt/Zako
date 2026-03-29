package zako.monolith.ticket;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import zako.monolith.ticket.dto.TicketRequest;
import zako.monolith.ticket.dto.TicketResponse;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse purchase(@RequestParam Long userId,
                                   @Valid @RequestBody TicketRequest request) {
        return ticketService.purchase(userId, request);
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id, @RequestParam Long userId) {
        return ticketService.cancel(id, userId);
    }

    @GetMapping("/user/{userId}")
    public List<TicketResponse> getUserTickets(@PathVariable Long userId) {
        return ticketService.getUserTickets(userId);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }
}
