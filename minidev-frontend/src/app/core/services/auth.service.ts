import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';

export type UserRole = 'ADMIN' | 'USER' | 'GUEST';

export interface User {
  id: string;
  username: string;
  displayName: string;
  role: UserRole;
  token?: string;
}

export interface UserRequest {
  username: string;
  password?: string;
  displayName?: string;
  role?: UserRole;
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
    const request: UserRequest = { username, password };
    return this.http.post<User>('/api/auth/login', request)
      .pipe(
        tap(user => {
          this.currentUserSignal.set(user);
          localStorage.setItem('user', JSON.stringify(user));
        })
      );
  }

  logout() {
    return this.http.post('/api/auth/logout', {}).pipe(
      tap(() => {
        this.currentUserSignal.set(null);
        localStorage.removeItem('user');
      })
    );
  }

  getUsers() {
    return this.http.get<User[]>('/api/admin/users');
  }

  createUser(user: UserRequest) {
    return this.http.post<User>('/api/admin/users', user);
  }

  updateUser(id: string, user: UserRequest) {
    return this.http.put<User>(`/api/admin/users/${id}`, user);
  }

  deleteUser(id: string) {
    return this.http.delete(`/api/admin/users/${id}`);
  }

  hasRole(role: UserRole): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    return user.role === role;
  }
}
