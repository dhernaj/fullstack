import { Component, computed, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { AuthService } from '../core/auth.service';
import { HasRoleDirective } from './has-role.directive';
import { Navbar } from './navbar';

@Component({
  imports: [Navbar, HasRoleDirective],
  template: `<app-navbar /><p *appHasRole="requeridos()" data-testid="solo-personal">Gestión académica</p>`,
})
class PantallaPrueba {
  readonly requeridos = signal<string | string[]>(['ADMIN', 'DOCENTE']);
}

describe('Menú y contenido según el rol', () => {
  let fixture: ComponentFixture<PantallaPrueba>;
  const rol = signal<string | null>(null);
  const auth = {
    autenticado: computed(() => rol() !== null),
    usuario: computed(() => rol()?.toLowerCase() ?? null),
    roles: computed(() => rol() ? [rol()!] : []),
    tieneRol: (...roles: string[]) => rol() !== null && roles.includes(rol()!),
    logout: vi.fn(() => rol.set(null)),
  };

  beforeEach(async () => {
    rol.set(null);
    auth.logout.mockClear();
    await TestBed.configureTestingModule({
      imports: [PantallaPrueba],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
    fixture = TestBed.createComponent(PantallaPrueba);
    fixture.detectChanges();
  });

  function enlaces() {
    return Array.from(fixture.nativeElement.querySelectorAll('a')).map((a: unknown) => (a as HTMLAnchorElement).textContent?.trim());
  }

  it('muestra solo la marca y el enlace al login si no hay sesión', () => {
    expect(enlaces()).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/login');
    expect(fixture.nativeElement.querySelector('button')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="solo-personal"]')).toBeNull();
  });

  it('muestra Cursos y Nuevo curso al administrador', () => {
    rol.set('ADMIN');
    fixture.detectChanges();
    expect(enlaces()).toContain('Cursos');
    expect(enlaces()).toContain('Nuevo curso');
    expect(fixture.nativeElement.textContent).toContain('admin · ADMIN');
    expect(fixture.nativeElement.querySelector('[data-testid="solo-personal"]')).not.toBeNull();
  });

  for (const tipo of ['DOCENTE', 'ESTUDIANTE']) {
    it(`oculta Nuevo curso a ${tipo}`, () => {
      rol.set(tipo);
      fixture.detectChanges();
      expect(enlaces()).toContain('Cursos');
      expect(enlaces()).not.toContain('Nuevo curso');
      const personal = fixture.nativeElement.querySelector('[data-testid="solo-personal"]');
      expect(personal !== null).toBe(tipo === 'DOCENTE');
    });
  }

  it('actualiza los permisos al cambiar el requisito sin duplicar contenido', () => {
    rol.set('ADMIN');
    fixture.detectChanges();
    fixture.componentInstance.requeridos.set('ESTUDIANTE');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="solo-personal"]')).toBeNull();
    fixture.componentInstance.requeridos.set('ADMIN');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[data-testid="solo-personal"]')).toHaveLength(1);
  });

  it('retira los enlaces y el contenido protegido al salir', () => {
    rol.set('ADMIN');
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(auth.logout).toHaveBeenCalledOnce();
    expect(enlaces()).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('[data-testid="solo-personal"]')).toBeNull();
  });
});
