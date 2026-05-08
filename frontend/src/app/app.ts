import { Component, inject } from '@angular/core';
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

  searchDate: Date = new Date();
  fromStation: StationDto | null = null;
  toStation: StationDto | null = null;

  results: TripResponseDto[] = [];
  searching = false;
  searched = false;
  searchError: string | null = null;

  submit() {
    this.searchError = null;
    if (!this.fromStation || !this.toStation) {
      this.searchError = 'Wybierz stację odjazdu i przyjazdu z listy.';
      return;
    }
    if (this.fromStation.id === this.toStation.id) {
      this.searchError = 'Stacja odjazdu i przyjazdu muszą być różne.';
      return;
    }
    this.searching = true;
    this.searched = true;
    const date = this.searchDate.toISOString().slice(0, 10);
    this.trips.search({
      fromStationId: this.fromStation.id,
      toStationId: this.toStation.id,
      date
    }).subscribe({
      next: list => { this.results = list; this.searching = false; },
      error: () => { this.searchError = 'Błąd wyszukiwania połączeń.'; this.searching = false; }
    });
  }
}
