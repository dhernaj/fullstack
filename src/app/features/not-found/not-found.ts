import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <div class="text-center my-5">
      <h1 class="display-4 nh-title">404</h1>
      <p class="lead">La página solicitada no existe.</p>
      @if (auth.autenticado()) {
        <a class="btn btn-nh" routerLink="/cursos">Volver a cursos</a>
      } @else {
        <a class="btn btn-nh" routerLink="/login">Ir al inicio de sesión</a>
      }
    </div>
  `,
})
export class NotFound {
  protected readonly auth = inject(AuthService);
}
