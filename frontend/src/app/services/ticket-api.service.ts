import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PurchaseRequest {
  tripId: number;
  fromStationId: number;
  toStationId: number;
  price: number;
}

export interface TicketDto {
  id: number;
  userId: number;
  trip: any;
  fromStation: { id: number; name: string; city: string; code: string };
  toStation: { id: number; name: string; city: string; code: string };
  seatNumber: number | null;
  price: number;
  status: string;
  paymentStatus: string;
  ticketCode: string;
  purchasedAt: string;
}

@Injectable({ providedIn: 'root' })
export class TicketApiService {
  private http = inject(HttpClient);

  purchase(req: PurchaseRequest): Observable<TicketDto> {
    return this.http.post<TicketDto>('/api/tickets/purchase', req);
  }

  pay(ticketId: number): Observable<TicketDto> {
    return this.http.post<TicketDto>(`/api/tickets/${ticketId}/pay`, {});
  }

  cancel(ticketId: number): Observable<TicketDto> {
    return this.http.post<TicketDto>(`/api/tickets/${ticketId}/cancel`, {});
  }

  getUserTickets(userId: number): Observable<TicketDto[]> {
    return this.http.get<TicketDto[]>(`/api/tickets/user/${userId}`);
  }
}
