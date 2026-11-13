import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
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

  it('vuelve a cursos tras el login y vacía la contraseña', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    llenarFormulario();
    respuesta.next({ accessToken: 'prueba', tokenType: 'Bearer', expiresIn: 3600, username: 'admin', roles: ['ADMIN'] });
    respuesta.complete();
    fixture.detectChanges();
    expect(navegar).toHaveBeenCalledWith('/cursos');
    expect(fixture.nativeElement.querySelector('#password').value).toBe('');
  });

  it('conserva una ruta interna de regreso', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture.componentRef.setInput('volver', '/cursos?page=2');
    llenarFormulario();
    respuesta.next({ accessToken: 'prueba', tokenType: 'Bearer', expiresIn: 3600, username: 'admin', roles: ['ADMIN'] });
    expect(navegar).toHaveBeenCalledWith('/cursos?page=2');
  });

  for (const volver of ['//externo.example', 'https://externo.example', '/login']) {
    it(`descarta el regreso no permitido: ${volver}`, () => {
      const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      fixture.componentRef.setInput('volver', volver);
      llenarFormulario();
      respuesta.next({ accessToken: 'prueba', tokenType: 'Bearer', expiresIn: 3600, username: 'admin', roles: ['ADMIN'] });
      expect(navegar).toHaveBeenCalledWith('/cursos');
    });
  }
});
