import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Wallpaper } from '../models/wallpaper.model';

/**
 * Service for managing wallpapers.
 */
@Injectable({
  providedIn: 'root'
})
export class WallpaperService {
  private http = inject(HttpClient);

  /**
   * Gets a random wallpaper.
   * @returns {Observable<Wallpaper>} An Observable of a random wallpaper.
   */
  public getRandomWallpaper(): Observable<Wallpaper> {
    return this.http.get<Wallpaper>('/api/wallpaper/random');
  }

  /**
   * Gets all wallpapers.
   * @returns {Observable<Wallpaper[]>} An Observable of the wallpaper list.
   */
  public getWallpapers(): Observable<Wallpaper[]> {
    return this.http.get<Wallpaper[]>('/api/wallpaper');
  }

  /**
   * Gets a wallpaper by id.
   * @param {number} id - The wallpaper id.
   * @returns {Observable<Wallpaper>} An Observable of the wallpaper.
   */
  public getWallpaper(id: number): Observable<Wallpaper> {
    return this.http.get<Wallpaper>(`/api/wallpaper/${id}`);
  }

  /**
   * Generates a new wallpaper.
   * @returns {Observable<void>} An Observable that completes on generation.
   */
  public generateWallpaper(): Observable<void> {
    return this.http.post<void>('/api/wallpaper/generate?count=1', {});
  }

  /**
   * Gets the most recently generated wallpaper.
   * @returns {Observable<Wallpaper>} An Observable of the new wallpaper.
   */
  public getNewWallpaper(): Observable<Wallpaper> {
    return this.http.get<Wallpaper>('/api/wallpaper/new');
  }

  /**
   * Deletes a wallpaper by id.
   * @param {number} id - The wallpaper id to delete.
   * @returns {Observable<void>} An Observable that completes on deletion.
   */
  public deleteWallpaper(id: number): Observable<void> {
    return this.http.delete<void>(`/api/wallpaper/${id}`);
  }
}
