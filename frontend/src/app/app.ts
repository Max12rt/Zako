import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderMenu } from './header-menu/header-menu';
import { DatePicker } from './date-picker/date-picker';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderMenu, DatePicker],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  searchDate: Date = new Date();
}
