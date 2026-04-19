import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
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
  private router = inject(Router);

  user = this.authService.currentUser;

  get displayName() { return this.user()?.displayName || ''; }
  get role() { return this.user()?.role || ''; }
  get isAdmin() { return this.authService.hasRole('ADMIN'); }

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']).then(() => {
        location.reload();
      });
    });
  }
}
