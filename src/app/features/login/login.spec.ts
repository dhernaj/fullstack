import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Subject } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { routes } from '../../app.routes';
import { AuthService } from '../../core/auth.service';
import { TokenResponse } from '../../core/models';
import { Login } from './login';

describe('Formulario de ingreso', () => {
  let fixture: ComponentFixture<Login>;
  let respuesta: Subject<TokenResponse>;
  const auth = { login: vi.fn(), autenticado: signal(false) };

  beforeEach(async () => {
    auth.autenticado.set(false);
    respuesta = new Subject<TokenResponse>();
    auth.login.mockReset().mockReturnValue(respuesta);
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter(routes, withComponentInputBinding()), { provide: AuthService, useValue: auth }],
    }).compileComponents();
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
  });

  function llenarFormulario() {
    for (const [id, valor] of [['username', 'admin'], ['password', 'clave-de-prueba']]) {
      const campo = fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    }
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  it('redirige la ruta inicial al login con carga diferida', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(harness.routeNativeElement?.textContent).toContain('Iniciar sesión');
  });

  it('no consulta la API si hay campos vacíos', () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
    expect(auth.login).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelectorAll('.is-invalid').length).toBe(2);
  });

  it('evita el doble envío y muestra el error 401', () => {
    llenarFormulario();
    llenarFormulario();
    expect(auth.login).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    respuesta.error(new HttpErrorResponse({ status: 401 }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Usuario o contraseña incorrectos');
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(false);
  });

  it('informa cuando no hay conexión', () => {
    llenarFormulario();
    respuesta.error(new HttpErrorResponse({ status: 0 }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('No se pudo iniciar sesión');
  });

  it('confirma el ingreso mientras el listado está pendiente y vacía la contraseña', () => {
    llenarFormulario();
    auth.autenticado.set(true);
    respuesta.next({ accessToken: 'prueba', tokenType: 'Bearer', expiresIn: 3600, username: 'admin', roles: ['ADMIN'] });
    respuesta.complete();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('Sesión iniciada');
    expect(fixture.nativeElement.querySelector('#password').value).toBe('');
    auth.autenticado.set(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
  });
});
