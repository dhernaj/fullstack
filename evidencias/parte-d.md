# Parte D — servicio y listado de cursos

Fecha local: 12 de noviembre de 2026. Pasos 18–20 de la guía.

## Implementación

- CursoService tipado para listar, obtener, crear, actualizar y eliminar.
- Parámetros page, size, sort y docente mediante HttpParams, de acuerdo con el backend.
- Listado con signals, búsqueda por docente tras 400 ms, filtros repetidos descartados y reemplazo de peticiones anteriores con switchMap.
- Cancelación de suscripciones al abandonar la pantalla con takeUntilDestroyed.
- Tabla con acciones para ADMIN y visualización sin edición para DOCENTE/ESTUDIANTE.
- Confirmación de eliminación, bloqueo de envíos simultáneos y mensaje Problem Details ante errores.
- Paginación con controles deshabilitados en los límites. Si la página queda vacía, vuelve a una página válida.
- Ruta /cursos protegida con authGuard. La ruta inicial lleva a cursos y, sin sesión, al login con parámetro volver. Tras el login se abre el listado.

## Adaptaciones

El tamaño inicial sigue siendo cinco. Se añadió un selector de dos, cinco o diez cursos por página para comprobar la paginación con los tres cursos de demostración sin modificar la base de datos. Los botones numéricos se limitan a una ventana de cinco páginas, acompañada de Anterior y Siguiente.

Los botones Nuevo curso y Editar siguen llegando al 404 hasta implementar el formulario en la parte E, tal como advierte la guía. Se retiró la confirmación temporal de la parte B: el login ahora navega a la ruta real.

La compilación inicial mostró un aviso por superar el presupuesto generado de 500 kB en 216 bytes. Se ajustó el umbral de advertencia a 550 kB; el límite de error de 1 MB se conserva. Esto documenta un ajuste del presupuesto, no una reducción del tamaño de la aplicación.

## Pruebas automáticas

`npm test -- --watch=false`, código de salida 0:

```text
Test Files  7 passed (7)
Tests       59 passed (59)
Start at    22:01:42
Duration    8.91s
```

24 casos adicionales respecto a la parte C: contrato HTTP del servicio, paginación y tamaño de página, espera y descarte de búsquedas repetidas, cancelación de consultas atrasadas, búsqueda desde página 0, resultados vacíos, reintento, acciones según rol, eliminación cancelada, error 409, doble envío, regreso de la última página, cancelación al abandonar la pantalla, guard en la ruta real y regreso seguro después del login.

Las operaciones de creación, actualización y eliminación y el error 409 se probaron con HttpTestingController, sin escribir datos reales.

## Comprobación en navegador con API real

Perfil H2 de demostración del Laboratorio 2. PostgreSQL no se modificó ni se presenta como verificado en esta etapa.

1. El estudiante abrió /cursos y vio los tres cursos sin botones Editar o Eliminar. Captura: listado-estudiante.png.
2. La búsqueda por Luis mostró únicamente SIS-205, Base de Datos, docente Luis Pérez. Captura: busqueda-docente.png.
3. Se vació el filtro y se seleccionó tamaño 2. Al pulsar Siguiente apareció SIS-101 en Página 2 de 2, con Siguiente deshabilitado. Captura: paginacion-segunda-pagina.png.
4. Se cerró sesión y se abrió /cursos: redirigió a /login?volver=%2Fcursos.
5. Se ingresó como admin con la contraseña local existente: volvió a /cursos y mostró las acciones de escritura. Captura: listado-admin.png.

El listado y sus filtros se consultaron contra la API protegida real. Los nombres de los parámetros y el encabezado Bearer se verifican en las pruebas HTTP automatizadas; no se inspeccionó DevTools manualmente. No se crearon ni eliminaron cursos durante la comprobación en navegador. La pestaña quedó en /cursos con la sesión admin.

Referencias consultadas: https://angular.dev/ecosystem/rxjs-interop/take-until-destroyed y https://rxjs.dev/api/operators/switchMap.

Siguiente etapa: parte E, pasos 21–22, formulario reactivo para crear y editar, exclusivo de ADMIN.

## Compilación final

`npm run build`, fuera del sandbox, código de salida 0 y sin advertencias:

```text
Initial total: 500.22 kB (transferencia estimada: 96.06 kB)
Lazy login: 44.52 kB
Lazy curso-list: 8.48 kB
Lazy not-found: 814 bytes
Application bundle generation complete. [3.950 seconds]
```

`git diff --check` pasó y el repositorio del backend permaneció sin cambios.
