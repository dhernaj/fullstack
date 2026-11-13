import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../environments/environment';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { TokenResponse } from './models';

const clave = 'academia.sesion';
const respuesta: TokenResponse = {
  accessToken: 'token-de-prueba', tokenType: 'Bearer', expiresIn: 3600,
  username: 'admin', roles: ['ADMIN', 'FACTOR_PASSWORD'],
};

describe('Sesión e interceptor JWT', () => {
  let http: HttpClient;
  let peticiones: HttpTestingController;
  const router = { navigate: vi.fn().mockResolvedValue(true) };

  beforeEach(() => {
    sessionStorage.clear();
    router.navigate.mockClear();
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authInterceptor])),
      provideHttpClientTesting(),
      { provide: Router, useValue: router },
    ] });
    http = TestBed.inject(HttpClient);
    peticiones = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    peticiones.verify();
    TestBed.resetTestingModule();
    sessionStorage.clear();
    vi.useRealTimers();
  });

  function ingresar() {
    const auth = TestBed.inject(AuthService);
    auth.login('admin', 'clave-solo-de-prueba').subscribe();
    const login = peticiones.expectOne(`${environment.apiUrl}/auth/login`);
    expect(login.request.body).toEqual({ username: 'admin', password: 'clave-solo-de-prueba' });
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush(respuesta);
    return auth;
  }

  it('guarda la respuesta real del backend, el usuario, los roles y la expiración', () => {
    const auth = ingresar();
    expect(auth.usuario()).toBe('admin');
    expect(auth.autenticado()).toBe(true);
    expect(auth.tieneRol('ADMIN')).toBe(true);
    expect(auth.tieneRol('DOCENTE')).toBe(false);
    expect(auth.roles()).toEqual(['ADMIN']);
    expect(auth.tieneRol('FACTOR_PASSWORD')).toBe(false);
    const almacenado = JSON.parse(sessionStorage.getItem(clave)!);
    expect(almacenado.expiresIn).toBe(3600);
    expect(almacenado.expiraEn).toBeGreaterThan(Date.now());
    expect(almacenado.password).toBeUndefined();
  });

  it('restaura una sesión vigente al recargar', () => {
    sessionStorage.setItem(clave, JSON.stringify({ ...respuesta, expiraEn: Date.now() + 60_000 }));
    expect(TestBed.inject(AuthService).token()).toBe(respuesta.accessToken);
  });

  for (const [nombre, crudo] of [
    ['JSON dañado', '{esto no es JSON'],
    ['sesión vencida', JSON.stringify({ ...respuesta, expiraEn: 1 })],
    ['sin fecha de expiración', JSON.stringify(respuesta)],
    ['contrato incompleto', JSON.stringify({ accessToken: 'viejo', expiraEn: Date.now() + 60_000 })],
  ]) {
    it(`descarta ${nombre} sin impedir el ingreso`, () => {
      sessionStorage.setItem(clave, crudo);
      expect(TestBed.inject(AuthService).autenticado()).toBe(false);
      expect(sessionStorage.getItem(clave)).toBeNull();
    });
  }

  it('no guarda una sesión cuando la contraseña es incorrecta', () => {
    const auth = TestBed.inject(AuthService);
    let estado = 0;
    auth.login('admin', 'incorrecta').subscribe({ error: (error) => estado = error.status });
    peticiones.expectOne(`${environment.apiUrl}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(estado).toBe(401);
    expect(auth.autenticado()).toBe(false);
    expect(sessionStorage.getItem(clave)).toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('cierra la sesión al vencer sin esperar otra petición', () => {
    vi.useFakeTimers();
    const auth = ingresar();
    vi.advanceTimersByTime(3_600_000);
    expect(auth.autenticado()).toBe(false);
    expect(auth.token()).toBeNull();
    expect(sessionStorage.getItem(clave)).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('adjunta Bearer a una petición de cursos', () => {
    ingresar();
    http.get(`${environment.apiUrl}/cursos`).subscribe();
    const consulta = peticiones.expectOne(`${environment.apiUrl}/cursos`);
    expect(consulta.request.headers.get('Authorization')).toBe(`Bearer ${respuesta.accessToken}`);
    consulta.flush({});
  });

  it('no envía el token sin una sesión', () => {
    http.get(`${environment.apiUrl}/cursos`).subscribe();
    const consulta = peticiones.expectOne(`${environment.apiUrl}/cursos`);
    expect(consulta.request.headers.has('Authorization')).toBe(false);
    consulta.flush({});
  });

  it('no envía el token a otro origen ni a una ruta con un prefijo parecido', () => {
    ingresar();
    const base = new URL(environment.apiUrl, window.location.origin);
    for (const url of ['https://externo.example/api/v1/cursos', `${base.origin}${base.pathname}-externa/cursos`]) {
      http.get(url).subscribe({ error: () => {} });
      const consulta = peticiones.expectOne(url);
      expect(consulta.request.headers.has('Authorization')).toBe(false);
      consulta.flush({}, { status: 401, statusText: 'Unauthorized' });
    }
    expect(TestBed.inject(AuthService).autenticado()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('limpia la sesión ante un 401 de un recurso protegido de la API', () => {
    const auth = ingresar();
    http.get(`${environment.apiUrl}/cursos`).subscribe({ error: () => {} });
    peticiones.expectOne(`${environment.apiUrl}/cursos`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.autenticado()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('no cierra una sesión nueva por un 401 de una petición anterior', () => {
    const auth = ingresar();
    http.get(`${environment.apiUrl}/cursos`).subscribe({ error: () => {} });
    const antigua = peticiones.expectOne(`${environment.apiUrl}/cursos`);
    auth.login('admin', 'clave-solo-de-prueba').subscribe();
    peticiones.expectOne(`${environment.apiUrl}/auth/login`).flush({ ...respuesta, accessToken: 'token-nuevo' });
    antigua.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.token()).toBe('token-nuevo');
    expect(auth.autenticado()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('conserva la sesión ante un 403 y no adjunta el token al login', () => {
    const auth = ingresar();
    http.get(`${environment.apiUrl}/cursos`).subscribe({ error: () => {} });
    peticiones.expectOne(`${environment.apiUrl}/cursos`).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.autenticado()).toBe(true);
    ingresar();
  });
});
