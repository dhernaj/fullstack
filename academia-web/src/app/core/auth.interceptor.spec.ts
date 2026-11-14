import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('Interceptor JWT aislado', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  const logout = vi.fn();
  const token = 'token-ficticio-del-interceptor';

  beforeEach(() => {
    logout.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { token: () => token, logout } },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adjunta Bearer a nuestra API y conserva las demás cabeceras', () => {
    http.get(`${environment.apiUrl}/cursos`, { headers: { 'X-Solicitud': 'prueba' } }).subscribe();
    const req = controller.expectOne(`${environment.apiUrl}/cursos`);
    expect(req.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    expect(req.request.headers.get('X-Solicitud')).toBe('prueba');
    req.flush({});
  });

  it('no envía el token a otro dominio ni cierra sesión por su 401', () => {
    const url = 'https://externo.example/datos';
    http.get(url).subscribe({ error: () => {} });
    const req = controller.expectOne(url);
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logout).not.toHaveBeenCalled();
  });

  it('cierra la sesión si su token recibe 401 de nuestra API', () => {
    http.get(`${environment.apiUrl}/cursos`).subscribe({ error: () => {} });
    controller.expectOne(`${environment.apiUrl}/cursos`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(logout).toHaveBeenCalledOnce();
  });
});
