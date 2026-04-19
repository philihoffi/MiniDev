import { Component, inject, signal, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { WallpaperService } from '../../core/services/wallpaper.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private wallpaperService = inject(WallpaperService);
  private sanitizer = inject(DomSanitizer);

  username = '';
  password = '';
  errorMessage = signal<string | null>(null);
  isLoading = signal(false);
  wallpaperCode = signal<SafeHtml | null>(null);

  ngOnInit() {
    this.loadWallpaper();
  }

  loadWallpaper() {
    this.wallpaperService.getLatestWallpaper().subscribe({
      next: (wp) => {
        this.wallpaperCode.set(this.sanitizer.bypassSecurityTrustHtml(wp.code));
      },
      error: (err) => {
        console.error('Failed to load wallpaper', err);
      }
    });
  }

  onSubmit() {
    if (!this.username || !this.password) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Invalid username or password');
        } else {
          this.errorMessage.set('An error occurred. Please try again later.');
        }
      }
    });
  }
}
