import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class PageService {
  private http = inject(HttpClient);

  public getPages(): Observable<Page[]> {
    return this.http.get<Page[]>('/api/pages');
  }
}