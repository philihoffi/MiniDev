import { Component, inject, computed, signal, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PageService } from '../../../core/services/page.service';
import { Page } from '../../../core/models/page.model';

/**
 * Main application layout with navigation header.
 */
@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent implements OnInit {
  private authService = inject(AuthService);
  private pageService = inject(PageService);
  private router = inject(Router);

  public user = this.authService.currentUser;
  public isFullBleed = computed(() => this.router.url.includes('wallpaper-gallery'));
  public mobileMenuOpen = signal(false);
  public userMenuOpen = signal(false);
  public isDark = signal(false);
  public navItems = signal<Page[]>([]);

  /**
   * Gets the display name of the current user.
   * @returns {string} The display name or empty string.
   */
  public get displayName(): string { return this.user()?.displayName || ''; }

  /**
   * Gets the role of the current user.
   * @returns {string} The role string or empty string.
   */
  public get role(): string { return this.user()?.role || ''; }

  /**
   * Checks if the current user has admin role.
   * @returns {boolean} True if the user is an admin.
   */
  public get isAdmin(): boolean { return this.authService.hasRole('ADMIN'); }

  /**
   * Initializes the component with saved theme preferences.
   */
  constructor() {
    this.isDark.set(localStorage.getItem('theme') === 'dark' ||
      (!localStorage.getItem('theme') && window.matchMedia('(prefers-color-scheme: dark)').matches));
    this.applyTheme();
  }

  ngOnInit(): void {
    this.loadNavItems();
  }

  private loadNavItems(): void {
    this.pageService.getPages().subscribe({
      next: (pages) => this.navItems.set(pages),
      error: () => this.navItems.set([])
    });
  }

  @HostListener('document:click', [''])
  public onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.user-menu-wrapper')) {
      this.userMenuOpen.set(false);
    }
  }

  /**
   * Toggles dark mode theme.
   */
  public toggleDarkMode(): void {
    this.isDark.update(v => !v);
    localStorage.setItem('theme', this.isDark() ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme(): void {
    document.documentElement.classList.toggle('dark', this.isDark());
  }

  /**
   * Toggles the mobile menu open state.
   */
  public toggleMobileMenu(): void {
    this.mobileMenuOpen.update(v => !v);
  }

  /**
   * Toggles the user menu open state.
   * @param {Event} event - The mouse event.
   */
  public toggleUserMenu(event: Event): void {
    event.stopPropagation();
    this.userMenuOpen.update(v => !v);
  }

  /**
   * Logs out the current user and navigates to the login page.
   */
  public logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']).then(() => {
        location.reload();
      });
    });
  }
}
