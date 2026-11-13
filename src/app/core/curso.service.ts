import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Curso, CursoRequest, Pagina } from './models';

@Injectable({ providedIn: 'root' })
export class CursoService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cursos`;

  listar(pagina = 0, tamanio = 5, docente = '', orden = 'nombre,asc'): Observable<Pagina<Curso>> {
    let params = new HttpParams().set('page', pagina).set('size', tamanio).set('sort', orden);
    if (docente.trim()) params = params.set('docente', docente.trim());
    return this.http.get<Pagina<Curso>>(this.url, { params });
  }

  obtener(id: number): Observable<Curso> {
    return this.http.get<Curso>(`${this.url}/${id}`);
  }

  crear(curso: CursoRequest): Observable<Curso> {
    return this.http.post<Curso>(this.url, curso);
  }

  actualizar(id: number, curso: CursoRequest): Observable<Curso> {
    return this.http.put<Curso>(`${this.url}/${id}`, curso);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
