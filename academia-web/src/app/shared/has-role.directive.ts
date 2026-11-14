import { Directive, TemplateRef, ViewContainerRef, effect, inject, input } from '@angular/core';

import { AuthService } from '../core/auth.service';

// Esta directiva adapta la pantalla al rol; los permisos reales los comprueba la API.
@Directive({ selector: '[appHasRole]' })
export class HasRoleDirective {
  private readonly auth = inject(AuthService);
  private readonly template = inject(TemplateRef);
  private readonly contenedor = inject(ViewContainerRef);

  readonly appHasRole = input.required<string | string[]>();

  constructor() {
    effect(() => {
      const requeridos = [this.appHasRole()].flat();
      this.contenedor.clear();
      if (this.auth.tieneRol(...requeridos)) {
        this.contenedor.createEmbeddedView(this.template);
      }
    });
  }
}
