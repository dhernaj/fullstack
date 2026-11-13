import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.autenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { volver: state.url } });
  }
  const roles = (route.data['roles'] as string[] | undefined) ?? [];
  if (roles.length > 0 && !auth.tieneRol(...roles)) {
    return router.createUrlTree(['/cursos']);
  }
  return true;
};
