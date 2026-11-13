import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, finalize, map, startWith, switchMap } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { CursoService } from '../../core/curso.service';
import { Curso, Pagina, ProblemDetail } from '../../core/models';
import { HasRoleDirective } from '../../shared/has-role.directive';

@Component({
  selector: 'app-curso-list',
  imports: [RouterLink, HasRoleDirective],
  templateUrl: './curso-list.html',
})
export class CursoList implements OnInit {
  private readonly cursoService = inject(CursoService);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly busqueda$ = new Subject<string>();
  private readonly solicitudes$ = new Subject<{ numero: number; filtro: string }>();

  protected readonly pagina = signal<Pagina<Curso> | null>(null);
  protected readonly cargando = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly errorCarga = signal<string | null>(null);
  protected readonly eliminando = signal<number | null>(null);
  protected readonly filtro = signal('');
  protected readonly tamanio = signal(5);

  ngOnInit(): void {
    this.solicitudes$.pipe(
      startWith({ numero: 0, filtro: '' }),
      // Una búsqueda nueva reemplaza la anterior para evitar resultados atrasados.
      switchMap(({ numero, filtro }) => {
        this.cargando.set(true);
        this.errorCarga.set(null);
        return this.cursoService.listar(numero, this.tamanio(), filtro).pipe(
          catchError(() => {
            this.pagina.set(null);
            this.errorCarga.set('No se pudo cargar la lista de cursos.');
            return EMPTY;
          }),
          finalize(() => this.cargando.set(false)),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((pagina) => {
      this.pagina.set(pagina);
      if (pagina.pagina > 0 && pagina.contenido.length === 0) {
        this.cargar(Math.max(0, pagina.totalPaginas - 1));
      }
    });

    this.busqueda$.pipe(
      map((texto) => texto.trim()),
      debounceTime(400),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((texto) => {
      this.filtro.set(texto);
      this.cargar(0);
    });
  }

  protected buscar(texto: string): void {
    this.busqueda$.next(texto);
  }

  protected cambiarTamanio(tamanio: number): void {
    if (![2, 5, 10].includes(tamanio)) return;
    this.tamanio.set(tamanio);
    this.cargar(0);
  }

  protected cargar(numero: number): void {
    const pagina = this.pagina();
    if (!Number.isInteger(numero) || numero < 0 || (pagina && numero > 0 && numero >= pagina.totalPaginas)) return;
    this.solicitudes$.next({ numero, filtro: this.filtro() });
  }

  protected paginas(): number[] {
    const pagina = this.pagina();
    if (!pagina) return [];
    const inicio = Math.max(0, Math.min(pagina.pagina - 2, pagina.totalPaginas - 5));
    return Array.from({ length: Math.min(5, pagina.totalPaginas) }, (_, i) => inicio + i);
  }

  protected eliminar(curso: Curso): void {
    if (!this.auth.tieneRol('ADMIN') || this.eliminando() !== null) return;
    if (!window.confirm(`¿Eliminar el curso ${curso.codigo} - ${curso.nombre}?`)) return;
    this.eliminando.set(curso.id);
    this.mensaje.set(null);
    this.cursoService.eliminar(curso.id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.eliminando.set(null)),
    ).subscribe({
      next: () => {
        this.mensaje.set(`Curso ${curso.codigo} eliminado.`);
        const pagina = this.pagina();
        const numero = pagina && pagina.contenido.length === 1 && pagina.pagina > 0
          ? pagina.pagina - 1 : pagina?.pagina ?? 0;
        this.cargar(numero);
      },
      error: (e: HttpErrorResponse) => {
        const problema = e.error as ProblemDetail | null;
        this.mensaje.set(problema?.detail ?? 'No se pudo eliminar el curso.');
      },
    });
  }
}
