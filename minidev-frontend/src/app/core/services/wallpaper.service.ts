import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';

export interface Wallpaper {
  code: string;
}

@Injectable({
  providedIn: 'root'
})
export class WallpaperService {
  private http = inject(HttpClient);

  getLatestWallpaper() {
    return this.http.get<Wallpaper>('/api/wallpaper/latest');
  }
}
