import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'cursos' },
  {
    path: 'login',
    title: 'Ingresar · Academia UPN',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: 'cursos',
    title: 'Cursos · Academia UPN',
    canActivate: [authGuard],
    loadComponent: () => import('./features/cursos/curso-list').then((m) => m.CursoList),
  },
  {
    path: '**',
    title: 'Página no encontrada · Academia UPN',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
];
