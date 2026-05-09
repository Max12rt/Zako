import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TicketApiService, TicketDto } from '../../services/ticket-api.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-my-tickets',
  imports: [CommonModule],
  templateUrl: './my-tickets.html',
  styleUrl: './my-tickets.scss'
})
export class MyTicketsPage implements OnInit {
  private ticketApi = inject(TicketApiService);
  private auth = inject(AuthService);
  private router = inject(Router);

  tickets = signal<TicketDto[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  ngOnInit() {
    const userId = this.auth.currentUser()?.id;
    if (!userId) { this.router.navigate(['/login']); return; }
    this.ticketApi.getUserTickets(userId).subscribe({
      next: t => { this.tickets.set(t); this.loading.set(false); },
      error: () => { this.error.set('Błąd ładowania biletów.'); this.loading.set(false); }
    });
  }

  cancel(ticket: TicketDto) {
    this.ticketApi.cancel(ticket.id).subscribe({
      next: updated => {
        this.tickets.update(list =>
          list.map(t => t.id === updated.id ? updated : t)
        );
      },
      error: () => this.error.set('Nie udało się anulować biletu.')
    });
  }

  goHome() { this.router.navigate(['/']); }
}
