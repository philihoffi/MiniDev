import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
import { Wallpaper } from '../models/wallpaper.model';

@Injectable({
  providedIn: 'root'
})
export class WallpaperService {
  private http = inject(HttpClient);

  getLatestWallpaper() {
    return this.http.get<Wallpaper>('/api/wallpaper/latest');
  }

  getWallpapers() {
    return this.http.get<Wallpaper[]>('/api/wallpaper');
  }

  getWallpaper(id: number) {
    return this.http.get<Wallpaper>(`/api/wallpaper/${id}`);
  }

  generateWallpaper() {
    return this.http.post<void>('/api/wallpaper/generate?count=1', {});
  }

  getNewWallpaper() {
    return this.http.get<Wallpaper>('/api/wallpaper/new');
  }

  deleteWallpaper(id: number) {
    return this.http.delete<void>(`/api/wallpaper/${id}`);
  }
}
