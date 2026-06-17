import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthApiService, LoginRequest, LoginResponse, RegisterRequest, UserDto } from './auth-api.service';
import { Observable, tap } from 'rxjs';
import { NotificationService } from '../notifications/notification.service';

const TOKEN_KEY = 'zako_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(AuthApiService);
  private router = inject(Router);
  private notif = inject(NotificationService);

  token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  currentUser = signal<UserDto | null>(null);
  isLoggedIn = computed(() => !!this.token());

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.api.login(req).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this.token.set(res.token);
        this.currentUser.set(res.user);
        this.notif.connect(res.token);
      })
    );
  }

  register(req: RegisterRequest): Observable<UserDto> {
    return this.api.register(req);
  }

  logout() {
    this.notif.disconnect();
    localStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/']);
  }

  loadCurrentUser() {
    if (this.token()) {
      this.api.me().subscribe({
        next: user => {
          this.currentUser.set(user);
          this.notif.connect(this.token()!);
        },
        error: () => this.logout()
      });
    }
  }
}
