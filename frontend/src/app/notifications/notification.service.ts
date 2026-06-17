import { Injectable, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';

export interface Toast {
  id: number;
  type: 'purchased' | 'cancelled';
  message: string;
  ticketCode: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  toasts = signal<Toast[]>([]);
  private client?: Client;
  private nextId = 0;

  connect(token: string) {
    if (this.client?.active) return;
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    this.client = new Client({
      brokerURL: `${proto}://${location.host}/ws?token=${token}`,
      reconnectDelay: 5000,
      onConnect: () => {
        this.client!.subscribe('/user/queue/notifications', msg => {
          const data = JSON.parse(msg.body);
          this.push(data);
        });
      }
    });
    this.client.activate();
  }

  disconnect() {
    this.client?.deactivate();
    this.client = undefined;
    this.toasts.set([]);
  }

  dismiss(id: number) {
    this.toasts.update(ts => ts.filter(t => t.id !== id));
  }

  private push(data: { type: string; message: string; ticketCode: string }) {
    const id = this.nextId++;
    this.toasts.update(ts => [...ts, {
      id,
      type: data.type as Toast['type'],
      message: data.message,
      ticketCode: data.ticketCode
    }]);
    setTimeout(() => this.dismiss(id), 5000);
  }
}
