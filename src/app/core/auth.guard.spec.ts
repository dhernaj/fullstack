import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { routes } from '../app.routes';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

@Component({ template: 'Listado de prueba' })
class ListadoPrueba {}
@Component({ template: 'Formulario de prueba' })
class FormularioPrueba {}

describe('Acceso a rutas y página 404', () => {
  const rol = signal<string | null>(null);
  const auth = {
    autenticado: () => rol() !== null,
    tieneRol: (...roles: string[]) => rol() !== null && roles.includes(rol()!),
  };

  beforeEach(() => {
    rol.set(null);
    TestBed.configureTestingModule({ providers: [
      { provide: AuthService, useValue: auth },
      provideRouter([
        // Estas pantallas de prueba permiten verificar el guard antes de las partes D y E.
        { path: 'cursos/nuevo', component: FormularioPrueba, canActivate: [authGuard], data: { roles: ['ADMIN'] } },
        { path: 'cursos', component: ListadoPrueba, canActivate: [authGuard] },
        ...routes,
      ], withComponentInputBinding()),
    ] });
  });

  it('envía al login y conserva la ruta completa cuando no hay sesión', async () => {
    const harness = await RouterTestingHarness.create('/cursos?page=2');
    const router = TestBed.inject(Router);
    expect(router.url).toBe('/login?volver=%2Fcursos%3Fpage%3D2');
    expect(harness.routeNativeElement?.textContent).toContain('Iniciar sesión');
  });

  for (const tipo of ['ADMIN', 'DOCENTE', 'ESTUDIANTE']) {
    it(`permite el listado a ${tipo}`, async () => {
      rol.set(tipo);
      const harness = await RouterTestingHarness.create('/cursos');
      expect(harness.routeNativeElement?.textContent).toBe('Listado de prueba');
    });
  }

  it('permite crear cursos al administrador', async () => {
    rol.set('ADMIN');
    const harness = await RouterTestingHarness.create('/cursos/nuevo');
    expect(harness.routeNativeElement?.textContent).toBe('Formulario de prueba');
  });

  for (const tipo of ['DOCENTE', 'ESTUDIANTE']) {
    it(`redirige a cursos si ${tipo} intenta abrir el formulario`, async () => {
      rol.set(tipo);
      const harness = await RouterTestingHarness.create('/cursos/nuevo');
      expect(TestBed.inject(Router).url).toBe('/cursos');
      expect(harness.routeNativeElement?.textContent).toBe('Listado de prueba');
    });
  }

  it('muestra 404 y un regreso al login sin sesión', async () => {
    const harness = await RouterTestingHarness.create('/xyz');
    expect(harness.routeNativeElement?.textContent).toContain('404');
    expect(harness.routeNativeElement?.querySelector('a')?.getAttribute('href')).toBe('/login');
  });

  it('ofrece volver a cursos desde el 404 con sesión iniciada', async () => {
    rol.set('ESTUDIANTE');
    const harness = await RouterTestingHarness.create('/xyz');
    expect(harness.routeNativeElement?.textContent).toContain('Volver a cursos');
    expect(harness.routeNativeElement?.querySelector('a')?.getAttribute('href')).toBe('/cursos');
  });
});
