import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const api = new URL(environment.apiUrl, window.location.origin);
  const destino = new URL(req.url, window.location.origin);
  const raiz = api.pathname.replace(/\/$/, '');
  // Comparar el origen y el límite de ruta evita aceptar dominios o prefijos parecidos.
  const esNuestraApi = destino.origin === api.origin &&
    (destino.pathname === raiz || destino.pathname.startsWith(`${raiz}/`));
  const esLogin = destino.pathname === `${raiz}/auth/login`;
  const token = esNuestraApi && !esLogin ? auth.token() : null;
  const peticion = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(peticion).pipe(catchError((error: HttpErrorResponse) => {
    if (error.status === 401 && esNuestraApi && !esLogin) auth.logout();
    return throwError(() => error);
  }));
};
