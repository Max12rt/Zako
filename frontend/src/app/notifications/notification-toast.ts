import { Component, inject } from '@angular/core';
import { NotificationService } from './notification.service';

@Component({
  selector: 'app-notification-toast',
  template: `
    <div class="toast-container">
      @for (toast of notif.toasts(); track toast.id) {
        <div class="toast" [class.purchased]="toast.type === 'purchased'" [class.cancelled]="toast.type === 'cancelled'">
          <div class="toast-body">
            <span>{{ toast.message }}</span>
            <small>{{ toast.ticketCode }}</small>
          </div>
          <button (click)="notif.dismiss(toast.id)" aria-label="Закрити">✕</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      bottom: 1.5rem;
      right: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      z-index: 9999;
    }
    .toast {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      color: #fff;
      font-size: 0.875rem;
      box-shadow: 0 4px 12px rgba(0,0,0,0.2);
      animation: slide-in 0.25s ease;
      min-width: 260px;
      max-width: 360px;
    }
    .toast.purchased { background: #22c55e; }
    .toast.cancelled  { background: #ef4444; }
    .toast-body { display: flex; flex-direction: column; gap: 0.125rem; flex: 1; }
    .toast small { opacity: 0.8; font-size: 0.75rem; font-family: monospace; }
    .toast button {
      background: none;
      border: none;
      color: #fff;
      cursor: pointer;
      font-size: 1rem;
      opacity: 0.8;
      flex-shrink: 0;
    }
    .toast button:hover { opacity: 1; }
    @keyframes slide-in {
      from { transform: translateX(110%); opacity: 0; }
      to   { transform: translateX(0);    opacity: 1; }
    }
  `]
})
export class NotificationToastComponent {
  notif = inject(NotificationService);
}
