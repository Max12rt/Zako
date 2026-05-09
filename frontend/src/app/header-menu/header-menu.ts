import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-header-menu',
  imports: [CommonModule, RouterLink],
  templateUrl: './header-menu.html',
  styleUrl: './header-menu.scss'
})
export class HeaderMenu {
  auth = inject(AuthService);
  private router = inject(Router);
  open = signal(false);

  toggle() { this.open.update(v => !v); }
  close() { this.open.set(false); }

  logout() {
    this.auth.logout();
    this.close();
  }

  goLogin() { this.router.navigate(['/login']); this.close(); }
  goRegister() { this.router.navigate(['/register']); this.close(); }
  goMyTickets() { this.router.navigate(['/my-tickets']); this.close(); }
}
