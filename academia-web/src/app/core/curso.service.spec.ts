import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../environments/environment';
import { CursoService } from './curso.service';
import { Curso, Pagina } from './models';

const datos = { codigo: 'SIS-999', nombre: 'Curso de prueba', creditos: 3, docente: 'Docente de prueba', cupos: 30 };

describe('Contrato HTTP de cursos', () => {
  let servicio: CursoService;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/cursos`;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    servicio = TestBed.inject(CursoService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('envía la paginación y el orden inicial sin un filtro vacío', () => {
    servicio.listar().subscribe();
    const req = http.expectOne((r) => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('5');
    expect(req.request.params.get('sort')).toBe('nombre,asc');
    expect(req.request.params.has('docente')).toBe(false);
    req.flush({ contenido: [], pagina: 0, tamanio: 5, totalElementos: 0, totalPaginas: 0, ultima: true });
  });

  it('envía la página, tamaño, orden y docente solicitados', () => {
    const respuesta: Pagina<Curso> = {
      contenido: [{ id: 7, ...datos, activo: true }], pagina: 1, tamanio: 10,
      totalElementos: 11, totalPaginas: 2, ultima: true,
    };
    let recibida: Pagina<Curso> | undefined;
    servicio.listar(1, 10, '  Pérez & Torres  ', 'creditos,desc').subscribe((pagina) => recibida = pagina);
    const req = http.expectOne((r) => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('sort')).toBe('creditos,desc');
    expect(req.request.params.get('docente')).toBe('Pérez & Torres');
    req.flush(respuesta);
    expect(recibida).toEqual(respuesta);
  });

  it('omite el filtro cuando solo contiene espacios', () => {
    servicio.listar(0, 5, '   ').subscribe();
    const req = http.expectOne((r) => r.url === base);
    expect(req.request.params.has('docente')).toBe(false);
    req.flush({ contenido: [], pagina: 0, tamanio: 5, totalElementos: 0, totalPaginas: 0, ultima: true });
  });

  it('obtiene un curso por id', () => {
    servicio.obtener(7).subscribe();
    const req = http.expectOne(`${base}/7`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, ...datos, activo: true });
  });

  for (const [metodo, ruta] of [['POST', base], ['PUT', `${base}/7`]]) {
    it(`envía los datos del curso mediante ${metodo}`, () => {
      (metodo === 'POST' ? servicio.crear(datos) : servicio.actualizar(7, datos)).subscribe();
      const req = http.expectOne(ruta);
      expect(req.request.method).toBe(metodo);
      expect(req.request.body).toEqual(datos);
      req.flush({ id: 7, ...datos, activo: true });
    });
  }

  it('elimina por id y admite la respuesta sin contenido', () => {
    servicio.eliminar(7).subscribe();
    const req = http.expectOne(`${base}/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
