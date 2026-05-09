import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-register',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class RegisterPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  firstName = signal('');
  lastName = signal('');
  email = signal('');
  password = signal('');
  error = signal<string | null>(null);
  loading = signal(false);

  submit() {
    this.error.set(null);
    this.loading.set(true);
    this.auth.register({
      firstName: this.firstName(),
      lastName: this.lastName(),
      email: this.email(),
      password: this.password()
    }).subscribe({
      next: () => {
        this.auth.login({ email: this.email(), password: this.password() }).subscribe({
          next: () => this.router.navigate(['/']),
          error: () => this.router.navigate(['/login'])
        });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Błąd rejestracji. Sprawdź dane.');
        this.loading.set(false);
      }
    });
  }
}
