import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WallpaperGalleryComponent } from './wallpaper-gallery.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('WallpaperGalleryComponent', () => {
  let component: WallpaperGalleryComponent;
  let fixture: ComponentFixture<WallpaperGalleryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WallpaperGalleryComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(WallpaperGalleryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
