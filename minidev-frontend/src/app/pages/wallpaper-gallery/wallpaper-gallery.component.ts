import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WallpaperService } from '../../core/services/wallpaper.service';
import { Wallpaper } from '../../core/models/wallpaper.model';
import { DomSanitizer, SafeHtml, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-wallpaper-gallery',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './wallpaper-gallery.component.html',
  styleUrl: './wallpaper-gallery.component.scss'
})
export class WallpaperGalleryComponent implements OnInit {
  private wallpaperService = inject(WallpaperService);
  private sanitizer = inject(DomSanitizer);

  wallpapers = signal<Wallpaper[]>([]);
  selectedWallpaper = signal<Wallpaper | null>(null);
  safeCode = signal<SafeHtml>('');
  isGenerating = signal<boolean>(false);

  ngOnInit() {
    this.loadWallpapers();
  }

  loadWallpapers() {
    this.wallpaperService.getWallpapers().subscribe(wps => {
      this.wallpapers.set(wps);
    });
  }

  generateNew() {
    this.isGenerating.set(true);
    this.wallpaperService.generateWallpaper().subscribe({
      next: () => {
        this.loadWallpapers();
        this.isGenerating.set(false);
      },
      error: () => {
        this.isGenerating.set(false);
        // Maybe add a toast/notification later if needed
      }
    });
  }

  selectWallpaper(wp: Wallpaper) {
    this.selectedWallpaper.set(wp);
    this.safeCode.set(this.sanitizer.bypassSecurityTrustHtml(wp.code));
  }
}
