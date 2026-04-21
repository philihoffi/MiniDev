import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { MainLayoutComponent } from './components/layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'user-management',
        loadComponent: () => import('./pages/user-management/user-management.component').then(m => m.UserManagementComponent),
        data: { role: 'ADMIN' }
      },
      {
        path: 'wallpaper-gallery',
        loadComponent: () => import('./pages/wallpaper-gallery/wallpaper-gallery.component').then(m => m.WallpaperGalleryComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
