import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    title: 'Ingresar · Academia UPN',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: '**',
    title: 'Página no encontrada · Academia UPN',
    loadComponent: () => import('./features/not-found/not-found').then((m) => m.NotFound),
  },
];
