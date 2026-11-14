import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthService } from '../core/auth.service';
import { HasRoleDirective } from './has-role.directive';

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, HasRoleDirective],
  template: `
    <nav class="navbar navbar-expand navbar-dark nh-navbar px-3 flex-wrap gap-2" aria-label="Navegación principal">
      <a class="navbar-brand fw-bold" [routerLink]="auth.autenticado() ? '/cursos' : '/login'">
        Academia <span class="text-nh-orange">UPN</span>
      </a>
      @if (auth.autenticado()) {
        <ul class="navbar-nav me-auto flex-wrap">
          <li class="nav-item">
            <a class="nav-link" routerLink="/cursos" routerLinkActive="active" ariaCurrentWhenActive="page"
              [routerLinkActiveOptions]="{ exact: true }">Cursos</a>
          </li>
          <li class="nav-item" *appHasRole="'ADMIN'">
            <a class="nav-link" routerLink="/cursos/nuevo" routerLinkActive="active" ariaCurrentWhenActive="page">Nuevo curso</a>
          </li>
        </ul>
        <span class="navbar-text small text-break">{{ auth.usuario() }} · {{ auth.roles().join(', ') }}</span>
        <button class="btn btn-sm btn-outline-light" type="button" (click)="auth.logout()">Salir</button>
      }
    </nav>
  `,
})
export class Navbar {
  protected readonly auth = inject(AuthService);
}
