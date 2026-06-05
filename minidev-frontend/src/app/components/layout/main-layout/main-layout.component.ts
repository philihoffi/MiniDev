import { Component, inject, computed, signal, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  user = this.authService.currentUser;
  isFullBleed = computed(() => this.router.url.includes('wallpaper-gallery'));
  mobileMenuOpen = signal(false);
  userMenuOpen = signal(false);
  isDark = signal(false);

  get displayName() { return this.user()?.displayName || ''; }
  get role() { return this.user()?.role || ''; }
  get isAdmin() { return this.authService.hasRole('ADMIN'); }

  constructor() {
    this.isDark.set(localStorage.getItem('theme') === 'dark' ||
      (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches));
    this.applyTheme();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu-wrapper')) {
      this.userMenuOpen.set(false);
    }
  }

  toggleDarkMode() {
    this.isDark.update(v => !v);
    localStorage.setItem('theme', this.isDark() ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme() {
    document.documentElement.classList.toggle('dark', this.isDark());
  }

  toggleMobileMenu() {
    this.mobileMenuOpen.update(v => !v);
  }

  toggleUserMenu(event: Event) {
    event.stopPropagation();
    this.userMenuOpen.update(v => !v);
  }

  logout() {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']).then(() => {
        location.reload();
      });
    });
  }
}
