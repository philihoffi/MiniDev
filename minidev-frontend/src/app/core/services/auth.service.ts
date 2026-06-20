import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { User, UserRequest, UserRole } from '../models/user.model';

/**
 * Authentication and user management service.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private currentUserSignal = signal<User | null>(null);

  public readonly currentUser = this.currentUserSignal.asReadonly();
  public readonly isAuthenticated = signal(false).asReadonly();

  /**
   * Initializes the service with saved user data from localStorage.
   */
  constructor() {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      this.currentUserSignal.set(JSON.parse(savedUser));
    }
  }

  /**
   * Authenticates a user with username and password.
   * @param {string} username - The username.
   * @param {string} password - The password.
   * @returns {Observable<User>} An Observable of the authenticated user.
   */
  public login(username: string, password: string): Observable<User> {
    const request: UserRequest = { username, password };
    return this.http.post<User>('/api/auth/login', request)
      .pipe(
        tap(user => {
          this.currentUserSignal.set(user);
          localStorage.setItem('user', JSON.stringify(user));
        })
      );
  }

  /**
   * Logs out the current user.
   * @returns {Observable<object>} An Observable that completes on logout.
   */
  public logout(): Observable<object> {
    return this.http.post('/api/auth/logout', {}).pipe(
      tap(() => {
        this.currentUserSignal.set(null);
        localStorage.removeItem('user');
      })
    );
  }

  /**
   * Gets all users (admin only).
   * @returns {Observable<User[]>} An Observable of the user list.
   */
  public getUsers(): Observable<User[]> {
    return this.http.get<User[]>('/api/admin/users');
  }

  /**
   * Creates a new user.
   * @param {UserRequest} user - The user data.
   * @returns {Observable<User>} An Observable of the created user.
   */
  public createUser(user: UserRequest): Observable<User> {
    return this.http.post<User>('/api/admin/users', user);
  }

  /**
   * Updates an existing user.
   * @param {string} id - The user ID.
   * @param {UserRequest} user - The updated user data.
   * @returns {Observable<User>} An Observable of the updated user.
   */
  public updateUser(id: string, user: UserRequest): Observable<User> {
    return this.http.put<User>(`/api/admin/users/${id}`, user);
  }

  /**
   * Deletes a user.
   * @param {string} id - The user ID to delete.
   * @returns {Observable<object>} An Observable that completes on deletion.
   */
  public deleteUser(id: string): Observable<object> {
    return this.http.delete(`/api/admin/users/${id}`);
  }

  /**
   * Checks if the current user has the specified role.
   * @param {UserRole} role - The role to check.
   * @returns {boolean} True if the user has the role.
   */
  public hasRole(role: UserRole): boolean {
    const user = this.currentUserSignal();
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    return user.role === role;
  }
}
