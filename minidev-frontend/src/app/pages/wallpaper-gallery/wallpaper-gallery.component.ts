import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WallpaperService } from '../../core/services/wallpaper.service';
import { Wallpaper } from '../../core/models/wallpaper.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ToastService } from '../../components/shared/toast/toast.service';
import { LoadingSpinnerComponent } from '../../components/shared/loading-spinner/loading-spinner.component';

/**
 * Wallpaper gallery page component.
 */
@Component({
  selector: 'app-wallpaper-gallery',
  standalone: true,
  imports: [CommonModule, LoadingSpinnerComponent],
  templateUrl: './wallpaper-gallery.component.html',
  styleUrl: './wallpaper-gallery.component.scss'
})
export class WallpaperGalleryComponent implements OnInit {
  private wallpaperService = inject(WallpaperService);
  private sanitizer = inject(DomSanitizer);
  private toast = inject(ToastService);

  public wallpapers = signal<Wallpaper[]>([]);
  public selectedWallpaper = signal<Wallpaper | null>(null);
  public safeCode = signal<SafeHtml>('');
  public isGenerating = signal(false);
  public loading = signal(true);
  public deleteConfirmId = signal<number | null>(null);

  /**
   * Angular lifecycle hook. Loads all wallpapers on init.
   */
  public ngOnInit(): void {
    this.loadWallpapers();
  }

  /**
   * Loads all wallpapers from the server.
   */
  public loadWallpapers(): void {
    this.loading.set(true);
    this.wallpaperService.getWallpapers().subscribe({
      next: (wps) => {
        this.wallpapers.set(wps);
        this.loading.set(false);
      },
      error: () => {
        this.toast.error('Failed to load wallpapers');
        this.loading.set(false);
      }
    });
  }

  /**
   * Generates a new wallpaper.
   */
  public generateNew(): void {
    this.isGenerating.set(true);
    this.wallpaperService.generateWallpaper().subscribe({
      next: () => {
        this.toast.success('New wallpaper generated');
        this.loadWallpapers();
        this.isGenerating.set(false);
      },
      error: () => {
        this.toast.error('Failed to generate wallpaper');
        this.isGenerating.set(false);
      }
    });
  }

  /**
   * Selects a wallpaper for preview.
   * @param {Wallpaper} wp - The wallpaper to select.
   */
  public selectWallpaper(wp: Wallpaper): void {
    this.selectedWallpaper.set(wp);
    this.safeCode.set(this.sanitizer.bypassSecurityTrustHtml(wp.code));
  }

  /**
   * Requests deletion confirmation for a wallpaper.
   * @param {Wallpaper} wp - The wallpaper to delete.
   * @param {Event} event - The mouse event.
   */
  public requestDelete(wp: Wallpaper, event: Event): void {
    event.stopPropagation();
    this.deleteConfirmId.set(wp.id);
  }

  /**
   * Cancels the deletion confirmation.
   */
  public cancelDelete(): void {
    this.deleteConfirmId.set(null);
  }

  /**
   * Confirms and executes wallpaper deletion.
   * @param {Wallpaper} wp - The wallpaper to delete.
   */
  public confirmDelete(wp: Wallpaper): void {
    this.deleteConfirmId.set(null);
    this.wallpaperService.deleteWallpaper(wp.id).subscribe({
      next: () => {
        this.toast.success(`"${wp.theme}" deleted`);
        this.loadWallpapers();
        if (this.selectedWallpaper()?.id === wp.id) {
          this.selectedWallpaper.set(null);
          this.safeCode.set('');
        }
      },
      error: () => {
        this.toast.error('Failed to delete wallpaper');
      }
    });
  }
}
