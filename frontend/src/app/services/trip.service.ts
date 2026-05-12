import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface TripSearchRequest {
  fromStationId: number;
  toStationId: number;
  date: string; // yyyy-MM-dd
}

export interface TripResponseDto {
  id: number;
  routeId: number;
  routeName: string;
  departureTime: string;
  arrivalTime: string;
  status: string;
  availableSeats: number;
  train: { id: number; number: string; type: string; capacity: number };
}

@Injectable({ providedIn: 'root' })
export class TripService {
  private http = inject(HttpClient);

  search(req: TripSearchRequest): Observable<TripResponseDto[]> {
    return this.http.post<TripResponseDto[]>('/api/trips/search', req);
  }
}
