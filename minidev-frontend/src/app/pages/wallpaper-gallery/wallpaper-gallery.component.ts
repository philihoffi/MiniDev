import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WallpaperService } from '../../core/services/wallpaper.service';
import { Wallpaper } from '../../core/models/wallpaper.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ToastService } from '../../components/shared/toast/toast.service';
import { LoadingSpinnerComponent } from '../../components/shared/loading-spinner/loading-spinner.component';

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

  wallpapers = signal<Wallpaper[]>([]);
  selectedWallpaper = signal<Wallpaper | null>(null);
  safeCode = signal<SafeHtml>('');
  isGenerating = signal(false);
  loading = signal(true);
  deleteConfirmId = signal<number | null>(null);

  ngOnInit() {
    this.loadWallpapers();
  }

  loadWallpapers() {
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

  generateNew() {
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

  selectWallpaper(wp: Wallpaper) {
    this.selectedWallpaper.set(wp);
    this.safeCode.set(this.sanitizer.bypassSecurityTrustHtml(wp.code));
  }

  requestDelete(wp: Wallpaper, event: Event) {
    event.stopPropagation();
    this.deleteConfirmId.set(wp.id);
  }

  cancelDelete() {
    this.deleteConfirmId.set(null);
  }

  confirmDelete(wp: Wallpaper) {
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
