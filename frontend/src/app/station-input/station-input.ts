import { Component, ElementRef, EventEmitter, HostListener, inject, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, of, Subject, switchMap, catchError } from 'rxjs';
import { StationDto, StationService } from '../services/station.service';

@Component({
  selector: 'app-station-input',
  imports: [FormsModule],
  templateUrl: './station-input.html',
  styleUrl: './station-input.scss'
})
export class StationInput {
  @Input() placeholder = '';
  @Input() showLocateButton = false;
  @Input() trailingIcon: 'pin' | 'chevron' = 'pin';
  @Output() stationSelected = new EventEmitter<StationDto>();

  private stations = inject(StationService);
  private host = inject(ElementRef<HTMLElement>);

  query = signal('');
  results = signal<StationDto[]>([]);
  selected = signal<StationDto | null>(null);
  open = signal(false);
  geoBusy = signal(false);
  geoError = signal<string | null>(null);
  highlight = signal(0);

  private input$ = new Subject<string>();

  constructor() {
    this.input$
      .pipe(
        debounceTime(180),
        distinctUntilChanged(),
        switchMap(q => q.trim().length < 1
          ? of([] as StationDto[])
          : this.stations.search(q.trim()).pipe(catchError(() => of([] as StationDto[]))))
      )
      .subscribe(list => {
        this.results.set(list.slice(0, 8));
        this.highlight.set(0);
      });
  }

  onInput(value: string) {
    this.query.set(value);
    this.selected.set(null);
    this.open.set(true);
    this.input$.next(value);
  }

  onFocus() {
    if (this.results().length > 0) this.open.set(true);
  }

  pick(s: StationDto) {
    this.selected.set(s);
    this.query.set(s.name);
    this.open.set(false);
    this.stationSelected.emit(s);
  }

  onKey(e: KeyboardEvent) {
    if (!this.open() || this.results().length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      this.highlight.update(i => Math.min(i + 1, this.results().length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      this.highlight.update(i => Math.max(i - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const r = this.results()[this.highlight()];
      if (r) this.pick(r);
    }
  }

  locate() {
    if (!navigator.geolocation) {
      this.geoError.set('Geolokalizacja niedostępna');
      return;
    }
    this.geoBusy.set(true);
    this.geoError.set(null);
    navigator.geolocation.getCurrentPosition(
      pos => {
        this.stations.nearest(pos.coords.latitude, pos.coords.longitude).subscribe({
          next: s => { this.pick(s); this.geoBusy.set(false); },
          error: () => { this.geoError.set('Brak najbliższej stacji'); this.geoBusy.set(false); }
        });
      },
      err => {
        this.geoError.set(err.code === 1 ? 'Brak zgody' : 'Błąd lokalizacji');
        this.geoBusy.set(false);
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60_000 }
    );
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!this.open()) return;
    if (!this.host.nativeElement.contains(e.target as Node)) this.open.set(false);
  }
}
