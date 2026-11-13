import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../../environments/environment';
import { routes } from '../../app.routes';
import { AuthService } from '../../core/auth.service';
import { CursoForm } from './curso-form';

const datos = { codigo: 'SIS-500', nombre: 'Pruebas de Software', creditos: 3, docente: 'Docente de prueba', cupos: 30 };
const curso = { id: 7, ...datos, activo: true };

describe('Formulario de cursos', () => {
  let fixture: ComponentFixture<CursoForm>;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/cursos`;
  const rol = signal<string | null>('ADMIN');
  const auth = {
    autenticado: () => rol() !== null,
    tieneRol: (...roles: string[]) => rol() !== null && roles.includes(rol()!),
  };

  beforeEach(async () => {
    rol.set('ADMIN');
    await TestBed.configureTestingModule({ imports: [CursoForm], providers: [
      provideHttpClient(), provideHttpClientTesting(), provideRouter(routes, withComponentInputBinding()),
      { provide: AuthService, useValue: auth },
    ] }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CursoForm);
  });

  afterEach(() => {
    fixture.destroy();
    http.verify({ ignoreCancelled: true });
    vi.restoreAllMocks();
  });

  function iniciar(id?: string) {
    if (id !== undefined) fixture.componentRef.setInput('id', id);
    fixture.detectChanges();
  }
  function campo(nombre: string, valor: string) {
    const input = fixture.nativeElement.querySelector(`#${nombre}`) as HTMLInputElement;
    input.value = valor; input.dispatchEvent(new Event('input'));
  }
  function llenar() {
    for (const [nombre, valor] of Object.entries(datos)) campo(nombre, String(valor));
  }
  function guardar() {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }

  it('marca todos los campos vacíos sin enviar una petición', () => {
    iniciar();
    for (const nombre of Object.keys(datos)) campo(nombre, '');
    guardar();
    expect(fixture.nativeElement.querySelectorAll('.is-invalid')).toHaveLength(5);
    http.expectNone((r) => r.method === 'POST');
  });

  for (const [nombre, valor] of [
    ['codigo', 'sis-500'], ['nombre', '   '], ['docente', '   '],
    ['nombre', 'n'.repeat(101)], ['docente', 'd'.repeat(81)],
    ['creditos', '0'], ['creditos', '7'], ['creditos', '1.5'],
    ['cupos', '0'], ['cupos', '61'], ['cupos', '2.5'],
  ]) {
    it(`rechaza un valor inválido en ${nombre}: ${valor.slice(0, 12)}`, () => {
      iniciar(); llenar(); campo(nombre, valor); guardar();
      expect(fixture.nativeElement.querySelector(`#${nombre}`).getAttribute('aria-invalid')).toBe('true');
      http.expectNone((r) => r.method === 'POST');
    });
  }

  it('crea una sola vez, recorta espacios y vuelve al listado', () => {
    iniciar(); llenar(); campo('nombre', '  Pruebas de Software  ');
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    guardar(); guardar();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(datos);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(true);
    req.flush(curso, { status: 201, statusText: 'Created' });
    expect(navegar).toHaveBeenCalledWith(['/cursos']);
  });

  it('muestra errores 400 por campo y deja corregirlos', () => {
    iniciar(); llenar(); guardar();
    http.expectOne(base).flush({ detail: 'Hay campos inválidos.', errores: { docente: 'El docente no está disponible.' } },
      { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#docente-error').textContent).toContain('El docente no está disponible.');
    expect(fixture.nativeElement.querySelector('#docente').getAttribute('aria-invalid')).toBe('true');
    campo('docente', 'Otro docente'); guardar();
    const req = http.expectOne(base);
    expect(req.request.body.docente).toBe('Otro docente');
    req.flush({ detail: 'Código repetido.' }, { status: 409, statusText: 'Conflict' });
  });

  it('muestra el detalle 409 y conserva los valores para corregirlos', () => {
    iniciar(); llenar(); guardar();
    http.expectOne(base).flush({ detail: 'Ya existe el código SIS-500.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Ya existe el código SIS-500.');
    expect(fixture.nativeElement.querySelector('#nombre').value).toBe(datos.nombre);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBe(false);
  });

  it('carga los datos para editar y envía PUT al mismo id', () => {
    iniciar('7');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    http.expectOne(`${base}/7`).flush(curso); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#nombre').value).toBe(datos.nombre);
    campo('nombre', 'Pruebas de Software II');
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    guardar();
    const req = http.expectOne(`${base}/7`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ ...datos, nombre: 'Pruebas de Software II' });
    req.flush({ ...curso, nombre: 'Pruebas de Software II' });
    expect(navegar).toHaveBeenCalledWith(['/cursos']);
  });

  for (const estado of [404, 500]) {
    it(`impide guardar cuando falla la lectura con ${estado}`, () => {
      iniciar('7'); http.expectOne(`${base}/7`).flush({}, { status: estado, statusText: 'Error' }); fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('form')).toBeNull();
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent)
        .toContain(estado === 404 ? 'El curso no existe.' : 'No se pudo cargar el curso.');
    });
  }

  for (const id of ['abc', '0', '-1', '1.5', '9007199254740992']) {
    it(`rechaza el id inválido ${id} sin consultar la API`, () => {
      iniciar(id);
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('identificador');
      expect(fixture.nativeElement.querySelector('form')).toBeNull();
      http.expectNone((r) => r.url.startsWith(base));
    });
  }

  it('cancela la lectura anterior al cambiar el parámetro id', () => {
    iniciar('7'); const anterior = http.expectOne(`${base}/7`);
    fixture.componentRef.setInput('id', '8'); fixture.detectChanges();
    expect(anterior.cancelled).toBe(true);
    http.expectOne(`${base}/8`).flush({ ...curso, id: 8, nombre: 'Otro curso' }); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#nombre').value).toBe('Otro curso');
  });

  it('cancela la lectura al abandonar el formulario', () => {
    iniciar('7'); const req = http.expectOne(`${base}/7`);
    fixture.destroy(); expect(req.cancelled).toBe(true);
  });

  for (const tipo of ['DOCENTE', 'ESTUDIANTE']) {
    for (const ruta of ['/cursos/nuevo', '/cursos/7/editar']) {
      it(`impide el acceso real a ${ruta} como ${tipo}`, async () => {
        rol.set(tipo);
        await RouterTestingHarness.create(ruta);
        expect(TestBed.inject(Router).url).toBe('/cursos');
        http.expectOne((r) => r.url === base && r.method === 'GET').flush({
          contenido: [], pagina: 0, tamanio: 5, totalElementos: 0, totalPaginas: 0, ultima: true,
        });
        http.expectNone(`${base}/7`);
      });
    }
  }

  it('carga el formulario real para ADMIN con el parámetro id de la ruta', async () => {
    const harness = await RouterTestingHarness.create('/cursos/7/editar');
    http.expectOne(`${base}/7`).flush(curso); harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector<HTMLInputElement>('#nombre')?.value).toBe(datos.nombre);
    expect(TestBed.inject(Router).url).toBe('/cursos/7/editar');
  });
});
