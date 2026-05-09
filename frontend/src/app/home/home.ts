import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HeaderMenu } from '../header-menu/header-menu';
import { DatePicker } from '../date-picker/date-picker';
import { StationInput } from '../station-input/station-input';
import { StationDto } from '../services/station.service';
import { TripResponseDto, TripService } from '../services/trip.service';

@Component({
  selector: 'app-home',
  imports: [CommonModule, HeaderMenu, DatePicker, StationInput],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class HomeComponent {
  private trips = inject(TripService);
  private router = inject(Router);

  searchDate = signal<Date>(new Date());
  fromStation = signal<StationDto | null>(null);
  toStation = signal<StationDto | null>(null);

  results = signal<TripResponseDto[]>([]);
  searching = signal(false);
  searched = signal(false);
  searchError = signal<string | null>(null);

  swapStations() {
    const a = this.fromStation();
    const b = this.toStation();
    this.fromStation.set(b);
    this.toStation.set(a);
  }

  goBook(tripId: number) {
    const from = this.fromStation();
    const to = this.toStation();
    this.router.navigate(['/book', tripId], {
      queryParams: { from: from?.id, to: to?.id }
    });
  }

  submit() {
    this.searchError.set(null);
    const from = this.fromStation();
    const to = this.toStation();
    if (!from || !to) {
      this.searchError.set('Wybierz stację odjazdu i przyjazdu z listy.');
      return;
    }
    if (from.id === to.id) {
      this.searchError.set('Stacja odjazdu i przyjazdu muszą być różne.');
      return;
    }
    this.searching.set(true);
    this.searched.set(true);
    const date = this.searchDate().toISOString().slice(0, 10);
    this.trips.search({ fromStationId: from.id, toStationId: to.id, date }).subscribe({
      next: list => { this.results.set(list); this.searching.set(false); },
      error: () => { this.searchError.set('Błąd wyszukiwania połączeń.'); this.searching.set(false); }
    });
  }
}
