import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const user = authService.currentUser();

  if (!user) {
    router.navigate(['/login']);
    return false;
  }

  // Check roles if defined in route data
  const requiredRole = route.data['role'];
  if (requiredRole && !authService.hasRole(requiredRole)) {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
