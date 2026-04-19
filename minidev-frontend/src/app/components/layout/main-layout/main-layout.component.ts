import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  standalone: true
})
export class MainLayoutComponent {
  private authService = inject(AuthService);

  user = this.authService.currentUser;

  get username() { return this.user()?.username || ''; }
  get role() { return this.user()?.role || ''; }
  get isAdmin() { return this.authService.hasRole('ADMIN'); }

  logout() {
    this.authService.logout();
    location.reload(); // Einfachster Weg um Guard zu triggern
  }
}
