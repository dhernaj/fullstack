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
import { Curso, Pagina } from '../../core/models';
import { CursoList } from './curso-list';

const cursos: Curso[] = Array.from({ length: 6 }, (_, i) => ({
  id: i + 1, codigo: `SIS-${100 + i}`, nombre: `Curso ${i + 1}`, creditos: 3,
  docente: 'Ana Torres', cupos: 30, activo: true,
}));
const pagina = (numero = 0): Pagina<Curso> => ({
  contenido: cursos.slice(numero * 5, numero * 5 + 5), pagina: numero, tamanio: 5,
  totalElementos: 6, totalPaginas: 2, ultima: numero === 1,
});

describe('Listado de cursos', () => {
  let fixture: ComponentFixture<CursoList>;
  let http: HttpTestingController;
  const rol = signal<string | null>('ADMIN');
  const base = `${environment.apiUrl}/cursos`;
  const auth = {
    autenticado: () => rol() !== null,
    tieneRol: (...roles: string[]) => rol() !== null && roles.includes(rol()!),
  };

  beforeEach(async () => {
    rol.set('ADMIN');
    await TestBed.configureTestingModule({
      imports: [CursoList],
      providers: [provideHttpClient(), provideHttpClientTesting(),
        provideRouter(routes, withComponentInputBinding()), { provide: AuthService, useValue: auth }],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CursoList);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    http.verify({ ignoreCancelled: true });
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  function consulta() { return http.expectOne((r) => r.url === base && r.method === 'GET'); }
  function cargarInicial() { consulta().flush(pagina()); fixture.detectChanges(); }
  function boton(texto: string): HTMLButtonElement {
    return Array.from(fixture.nativeElement.querySelectorAll('button')).find((b) =>
      (b as HTMLButtonElement).textContent?.trim() === texto) as HTMLButtonElement;
  }
  function buscar(texto: string) {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = texto;
    input.dispatchEvent(new Event('input'));
  }

  it('muestra los cursos y acciones del administrador', () => {
    cargarInicial();
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(5);
    expect(fixture.nativeElement.textContent).toContain('6 curso(s) en total');
    expect(fixture.nativeElement.querySelectorAll('a[aria-label^="Editar"]')).toHaveLength(5);
    expect(fixture.nativeElement.querySelectorAll('button[aria-label^="Eliminar"]')).toHaveLength(5);
  });

  it('espera 400 ms y consulta una vez por filtro distinto', () => {
    vi.useFakeTimers();
    cargarInicial();
    buscar(' Pér');
    vi.advanceTimersByTime(250);
    buscar(' Pérez ');
    vi.advanceTimersByTime(399);
    http.expectNone((r) => r.url === base);
    vi.advanceTimersByTime(1);
    const req = consulta();
    expect(req.request.params.get('docente')).toBe('Pérez');
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ ...pagina(), contenido: [], totalElementos: 0, totalPaginas: 0, ultima: true });
    buscar('Pérez');
    vi.advanceTimersByTime(400);
    http.expectNone((r) => r.url === base);
  });

  it('cancela una consulta anterior para no mostrar resultados atrasados', () => {
    vi.useFakeTimers();
    const anterior = consulta();
    buscar('Luis');
    vi.advanceTimersByTime(400);
    expect(anterior.cancelled).toBe(true);
    const actual = consulta();
    actual.flush({ ...pagina(), contenido: [cursos[1]], totalElementos: 1, totalPaginas: 1, ultima: true });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(1);
    expect(fixture.nativeElement.textContent).toContain('Curso 2');
  });

  it('cambia de página y deshabilita los límites', () => {
    cargarInicial();
    expect(boton('Anterior').disabled).toBe(true);
    boton('Siguiente').click();
    const req = consulta();
    expect(req.request.params.get('page')).toBe('1');
    req.flush(pagina(1));
    fixture.detectChanges();
    expect(boton('Siguiente').disabled).toBe(true);
    expect(boton('Anterior').disabled).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Página 2 de 2');
    boton('Siguiente').click();
    http.expectNone((r) => r.url === base);
  });

  it('consulta con el tamaño elegido y vuelve a la primera página', () => {
    cargarInicial();
    const selector = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    selector.value = '2'; selector.dispatchEvent(new Event('change'));
    const req = consulta();
    expect(req.request.params.get('size')).toBe('2');
    expect(req.request.params.get('page')).toBe('0');
    req.flush({ ...pagina(), contenido: cursos.slice(0, 2), tamanio: 2, totalPaginas: 3 });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(2);
  });

  it('vuelve a la primera página al cambiar el filtro', () => {
    vi.useFakeTimers();
    cargarInicial();
    boton('Siguiente').click(); consulta().flush(pagina(1)); fixture.detectChanges();
    buscar('Ana'); vi.advanceTimersByTime(400);
    const req = consulta();
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('docente')).toBe('Ana');
    req.flush(pagina());
  });

  it('informa cuando no hay resultados', () => {
    consulta().flush({ contenido: [], pagina: 0, tamanio: 5, totalElementos: 0, totalPaginas: 0, ultima: true });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No se encontraron cursos.');
    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });

  it('permite reintentar tras un error sin dejar una alerta antigua', () => {
    consulta().flush({}, { status: 500, statusText: 'Server error' }); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    boton('Reintentar').click(); consulta().flush(pagina()); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('table')).not.toBeNull();
  });

  it('oculta las acciones de escritura al estudiante', () => {
    rol.set('ESTUDIANTE'); cargarInicial();
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(5);
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
    expect(fixture.nativeElement.querySelector('button[aria-label^="Eliminar"]')).toBeNull();
  });

  it('no elimina si se cancela la confirmación', () => {
    cargarInicial(); vi.spyOn(window, 'confirm').mockReturnValue(false);
    (fixture.nativeElement.querySelector('button[aria-label^="Eliminar"]') as HTMLButtonElement).click();
    http.expectNone((r) => r.method === 'DELETE');
  });

  it('muestra el detalle de un 409 y conserva el curso', () => {
    cargarInicial(); vi.spyOn(window, 'confirm').mockReturnValue(true);
    (fixture.nativeElement.querySelector('button[aria-label^="Eliminar"]') as HTMLButtonElement).click();
    http.expectOne(`${base}/1`).flush({ detail: 'El curso tiene matrículas.' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('El curso tiene matrículas.');
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(5);
  });

  it('impide el doble borrado y vuelve a la página anterior si queda vacía', () => {
    cargarInicial(); boton('Siguiente').click(); consulta().flush(pagina(1)); fixture.detectChanges();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const eliminar = fixture.nativeElement.querySelector('button[aria-label^="Eliminar"]') as HTMLButtonElement;
    eliminar.click(); eliminar.click();
    const req = http.expectOne(`${base}/6`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    const recarga = consulta(); expect(recarga.request.params.get('page')).toBe('0');
    recarga.flush({ ...pagina(), totalElementos: 5, totalPaginas: 1, ultima: true }); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('eliminado.');
  });

  it('cancela las peticiones cuando se abandona la pantalla', () => {
    const req = consulta(); fixture.destroy(); expect(req.cancelled).toBe(true);
  });

  it('protege la ruta real y conserva el regreso al login', async () => {
    cargarInicial(); rol.set(null);
    const harness = await RouterTestingHarness.create('/cursos');
    expect(TestBed.inject(Router).url).toBe('/login?volver=%2Fcursos');
    expect(harness.routeNativeElement?.textContent).toContain('Iniciar sesión');
  });
});
