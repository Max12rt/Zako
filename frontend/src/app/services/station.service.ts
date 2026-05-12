import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface StationDto {
  id: number;
  name: string;
  city: string;
  code: string;
  latitude: number | null;
  longitude: number | null;
}

@Injectable({ providedIn: 'root' })
export class StationService {
  private http = inject(HttpClient);

  search(query: string): Observable<StationDto[]> {
    const q = encodeURIComponent(query);
    return this.http.get<StationDto[]>(`/api/stations/search?query=${q}`);
  }

  nearest(lat: number, lon: number): Observable<StationDto> {
    return this.http.get<StationDto>(`/api/stations/nearest?lat=${lat}&lon=${lon}`);
  }
}
