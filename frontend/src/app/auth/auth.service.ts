import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthApiService, LoginRequest, LoginResponse, RegisterRequest, UserDto } from './auth-api.service';
import { Observable, tap } from 'rxjs';

const TOKEN_KEY = 'zako_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = inject(AuthApiService);
  private router = inject(Router);

  token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  currentUser = signal<UserDto | null>(null);
  isLoggedIn = computed(() => !!this.token());

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.api.login(req).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        this.token.set(res.token);
        this.currentUser.set(res.user);
      })
    );
  }

  register(req: RegisterRequest): Observable<UserDto> {
    return this.api.register(req);
  }

  logout() {
    localStorage.removeItem(TOKEN_KEY);
    this.token.set(null);
    this.currentUser.set(null);
    this.router.navigate(['/']);
  }

  loadCurrentUser() {
    if (this.token()) {
      this.api.me().subscribe({
        next: user => this.currentUser.set(user),
        error: () => this.logout()
      });
    }
  }
}
