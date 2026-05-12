package zako.monolith.ticket;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import zako.monolith.ticket.dto.TicketRequest;
import zako.monolith.ticket.dto.TicketResponse;
import zako.monolith.user.User;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse purchase(@Valid @RequestBody TicketRequest request,
                                   Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.purchase(user.getId(), request);
    }

    @PostMapping("/{id}/pay")
    public TicketResponse pay(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.pay(id, user.getId());
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancel(@PathVariable Long id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.cancel(id, user.getId());
    }

    @GetMapping("/my")
    public List<TicketResponse> getMyTickets(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ticketService.getUserTickets(user.getId());
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }
}
