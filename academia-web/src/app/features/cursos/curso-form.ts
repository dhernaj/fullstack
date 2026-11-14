import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { CursoService } from '../../core/curso.service';
import { ProblemDetail } from '../../core/models';

const noBlanco: ValidatorFn = (control) => typeof control.value === 'string' && !control.value.trim()
  ? { required: true } : null;
const INICIAL = { codigo: '', nombre: '', creditos: 3, docente: '', cupos: 30 };

@Component({
  selector: 'app-curso-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './curso-form.html',
})
export class CursoForm {
  private readonly fb = inject(FormBuilder);
  private readonly cursoService = inject(CursoService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();
  protected readonly guardando = signal(false);
  protected readonly cargando = signal(false);
  protected readonly cargado = signal(true);
  protected readonly errorGeneral = signal<string | null>(null);
  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[A-Z]{3}-\d{3}$/)]],
    nombre: ['', [Validators.required, noBlanco, Validators.maxLength(100)]],
    creditos: [3, [Validators.required, Validators.min(1), Validators.max(6), Validators.pattern(/^\d+$/)]],
    docente: ['', [Validators.required, noBlanco, Validators.maxLength(80)]],
    cupos: [30, [Validators.required, Validators.min(1), Validators.max(60), Validators.pattern(/^\d+$/)]],
  });

  constructor() {
    effect((onCleanup) => {
      const id = this.id();
      untracked(() => {
        this.form.reset(INICIAL);
        this.errorGeneral.set(null);
        this.cargado.set(id === undefined);
        this.cargando.set(false);
        if (id === undefined) return;
        if (!/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) {
          this.errorGeneral.set('El identificador del curso no es válido.');
          return;
        }
        this.cargando.set(true);
        const consulta = this.cursoService.obtener(Number(id)).subscribe({
          next: ({ codigo, nombre, creditos, docente, cupos }) => {
            this.form.setValue({ codigo, nombre, creditos, docente, cupos });
            this.cargado.set(true);
            this.cargando.set(false);
          },
          error: (e: HttpErrorResponse) => {
            this.cargando.set(false);
            this.errorGeneral.set(e.status === 404 ? 'El curso no existe.' : 'No se pudo cargar el curso. Vuelve a intentarlo desde el listado.');
          },
        });
        // También cancela la lectura si se cambia a otro id sin destruir el componente.
        onCleanup(() => consulta.unsubscribe());
      });
    });
  }

  protected get esEdicion(): boolean { return this.id() !== undefined; }

  protected invalido(campo: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  protected mensajeCampo(campo: keyof typeof this.form.controls): string {
    const servidor = this.form.controls[campo].errors?.['servidor'];
    if (typeof servidor === 'string') return servidor;
    return {
      codigo: 'Usa tres letras mayúsculas, un guion y tres números: SIS-101.',
      nombre: 'Ingresa un nombre de hasta 100 caracteres.',
      docente: 'Ingresa un docente de hasta 80 caracteres.',
      creditos: 'Ingresa un número entero entre 1 y 6.',
      cupos: 'Ingresa un número entero entre 1 y 60.',
    }[campo];
  }

  protected guardar(): void {
    if (this.guardando() || this.cargando() || !this.cargado()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.errorGeneral.set(null);
    const valores = this.form.getRawValue();
    const datos = { ...valores, nombre: valores.nombre.trim(), docente: valores.docente.trim() };
    const id = this.id();
    const peticion = id === undefined ? this.cursoService.crear(datos) : this.cursoService.actualizar(Number(id), datos);
    peticion.pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.guardando.set(false)),
    ).subscribe({
      next: () => { void this.router.navigate(['/cursos']); },
      error: (e: HttpErrorResponse) => {
        const problema = e.error as ProblemDetail | null;
        for (const campo of Object.keys(this.form.controls) as (keyof typeof this.form.controls)[]) {
          const mensaje = problema?.errores?.[campo];
          if (typeof mensaje === 'string') {
            this.form.controls[campo].setErrors({ servidor: mensaje });
            this.form.controls[campo].markAsTouched();
          }
        }
        this.errorGeneral.set(problema?.detail ?? 'No se pudo guardar el curso. Inténtalo de nuevo.');
      },
    });
  }
}
