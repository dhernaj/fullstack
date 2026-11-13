import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `
    <main class="container py-4">
      <h1 class="nh-title">Academia UPN</h1>
      <router-outlet />
    </main>
  `,
})
export class App {}
