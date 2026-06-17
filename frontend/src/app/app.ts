import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationToastComponent } from './notifications/notification-toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NotificationToastComponent],
  template: '<router-outlet /><app-notification-toast />'
})
export class App {}
