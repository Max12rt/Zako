import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TripResponseDto } from '../../services/trip.service';
import { TicketApiService } from '../../services/ticket-api.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-book',
  imports: [CommonModule],
  templateUrl: './book.html',
  styleUrl: './book.scss'
})
export class BookPage implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  private ticketApi = inject(TicketApiService);
  private auth = inject(AuthService);

  trip = signal<TripResponseDto | null>(null);
  fromStationId = signal<number>(0);
  toStationId = signal<number>(0);
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal(false);

  ngOnInit() {
    const tripId = Number(this.route.snapshot.paramMap.get('tripId'));
    const from = Number(this.route.snapshot.queryParamMap.get('from'));
    const to = Number(this.route.snapshot.queryParamMap.get('to'));
    this.fromStationId.set(from);
    this.toStationId.set(to);
    this.http.get<TripResponseDto>(`/api/trips/${tripId}`).subscribe({
      next: t => this.trip.set(t),
      error: () => this.error.set('Nie znaleziono połączenia.')
    });
  }

  pay() {
    const t = this.trip();
    if (!t) return;
    this.loading.set(true);
    this.error.set(null);
    const basePrice = 29.90;
    this.ticketApi.purchase({
      tripId: t.id,
      fromStationId: this.fromStationId(),
      toStationId: this.toStationId(),
      price: basePrice
    }).subscribe({
      next: ticket => {
        this.ticketApi.pay(ticket.id).subscribe({
          next: () => { this.success.set(true); this.loading.set(false); },
          error: () => { this.error.set('Błąd płatności.'); this.loading.set(false); }
        });
      },
      error: () => { this.error.set('Nie udało się zarezerwować biletu.'); this.loading.set(false); }
    });
  }

  goMyTickets() { this.router.navigate(['/my-tickets']); }
  goBack() { this.router.navigate(['/']); }
}
