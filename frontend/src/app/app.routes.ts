import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { LoginPage } from './pages/login/login';
import { RegisterPage } from './pages/register/register';
import { BookPage } from './pages/book/book';
import { MyTicketsPage } from './pages/my-tickets/my-tickets';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: 'book/:tripId', component: BookPage, canActivate: [authGuard] },
  { path: 'my-tickets', component: MyTicketsPage, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
