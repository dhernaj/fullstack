import { HttpClient } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { environment } from '../../environments/environment';
import { TokenResponse } from './models';

const CLAVE = 'academia.sesion';
type Sesion = TokenResponse & { expiraEn: number };

function esToken(datos: unknown): datos is TokenResponse {
  if (!datos || typeof datos !== 'object') return false;
  const d = datos as Record<string, unknown>;
  return typeof d['accessToken'] === 'string' && d['accessToken'].length > 0 &&
    d['tokenType'] === 'Bearer' && typeof d['username'] === 'string' &&
    typeof d['expiresIn'] === 'number' && Number.isFinite(d['expiresIn']) && d['expiresIn'] > 0 &&
    Array.isArray(d['roles']) && d['roles'].every((rol) => typeof rol === 'string');
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly sesion = signal<Sesion | null>(this.leerSesion());
  private temporizador?: ReturnType<typeof setTimeout>;

  readonly usuario = computed(() => this.sesion()?.username ?? null);
  readonly roles = computed(() => this.sesion()?.roles ?? []);
  readonly autenticado = computed(() => this.sesion() !== null);

  constructor() {
    this.programarVencimiento();
    inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizador));
  }

  login(username: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>(`${environment.apiUrl}/auth/login`, { username, password })
      .pipe(tap((respuesta) => {
        if (!esToken(respuesta)) throw new Error('Respuesta de sesión no válida');
        const sesion = { ...respuesta, expiraEn: Date.now() + respuesta.expiresIn * 1000 };
        sessionStorage.setItem(CLAVE, JSON.stringify(sesion));
        this.sesion.set(sesion);
        this.programarVencimiento();
      }));
  }

  logout(): void {
    clearTimeout(this.temporizador);
    sessionStorage.removeItem(CLAVE);
    this.sesion.set(null);
    void this.router.navigate(['/login']);
  }

  token(): string | null {
    const sesion = this.sesion();
    if (sesion && Date.now() >= sesion.expiraEn) {
      this.logout();
      return null;
    }
    return sesion?.accessToken ?? null;
  }

  tieneRol(...roles: string[]): boolean {
    return this.autenticado() && roles.some((rol) => this.roles().includes(rol));
  }

  private programarVencimiento(): void {
    clearTimeout(this.temporizador);
    const sesion = this.sesion();
    if (sesion) {
      // El temporizador también actualiza la pantalla si no hay nuevas peticiones.
      this.temporizador = setTimeout(() => this.logout(), Math.min(sesion.expiraEn - Date.now(), 2_147_483_647));
    }
  }

  private leerSesion(): Sesion | null {
    try {
      const datos: unknown = JSON.parse(sessionStorage.getItem(CLAVE) ?? 'null');
      if (esToken(datos) && 'expiraEn' in datos &&
          typeof datos.expiraEn === 'number' && Number.isFinite(datos.expiraEn) && datos.expiraEn > Date.now()) {
        return datos as Sesion;
      }
    } catch {
      // Una sesión dañada se descarta para que el usuario pueda volver a ingresar.
    }
    sessionStorage.removeItem(CLAVE);
    return null;
  }
}
