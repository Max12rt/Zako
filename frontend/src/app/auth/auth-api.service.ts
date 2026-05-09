import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface LoginRequest { email: string; password: string; }
export interface RegisterRequest { firstName: string; lastName: string; email: string; password: string; }
export interface UserDto { id: number; firstName: string; lastName: string; email: string; role: string; }
export interface LoginResponse { token: string; user: UserDto; }

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private http = inject(HttpClient);

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', req);
  }

  register(req: RegisterRequest): Observable<UserDto> {
    return this.http.post<UserDto>('/api/users/register', req);
  }

  me(): Observable<UserDto> {
    return this.http.get<UserDto>('/api/auth/me');
  }
}
