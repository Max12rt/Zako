package zako.ticket.ticket;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import zako.ticket.ticket.dto.TicketRequest;
import zako.ticket.ticket.dto.TicketResponse;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse purchase(@Valid @RequestBody TicketRequest request, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ticketService.purchase(userId, request);
    }

    @PostMapping("/{id}/pay")
    public TicketResponse pay(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ticketService.pay(id, userId);
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ticketService.cancel(id, userId);
    }

    @GetMapping("/my")
    public List<TicketResponse> getMyTickets(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ticketService.getUserTickets(userId);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }
}
