import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderMenu } from './header-menu/header-menu';
import { DatePicker } from './date-picker/date-picker';
import { StationInput } from './station-input/station-input';
import { StationDto } from './services/station.service';
import { TripResponseDto, TripService } from './services/trip.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, HeaderMenu, DatePicker, StationInput],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private trips = inject(TripService);

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
    this.trips.search({
      fromStationId: from.id,
      toStationId: to.id,
      date
    }).subscribe({
      next: list => { this.results.set(list); this.searching.set(false); },
      error: () => { this.searchError.set('Błąd wyszukiwania połączeń.'); this.searching.set(false); }
    });
  }
}
