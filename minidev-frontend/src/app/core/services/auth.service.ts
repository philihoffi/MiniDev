import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

export type UserRole = 'ADMIN' | 'USER' | 'GUEST';

export interface User {
  id: string;
  username: string;
  role: UserRole;
  token?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private currentUserSignal = signal<User | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = signal(false).asReadonly();

  constructor() {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      this.currentUserSignal.set(JSON.parse(savedUser));
    }
  }

  login(username: string, password: string) {
    return this.http.post<User>('/api/auth/login', { username, password })
      .pipe(
        tap(user => {
          this.currentUserSignal.set(user);
          localStorage.setItem('user', JSON.stringify(user));
        })
      );
  }

  logout() {
    this.currentUserSignal.set(null);
    localStorage.removeItem('user');
  }

  getUsers() {
    return this.http.get<User[]>('/api/admin/users');
  }

  hasRole(role: UserRole): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    return user.role === role;
  }
}
