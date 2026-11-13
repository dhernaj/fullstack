# Parte A — proyecto, Bootstrap y entornos

Fecha local: 12 de noviembre de 2026. Pasos 1–6 de la guía.

## Entorno comprobado

- macOS Intel (darwin x64).
- Node.js 24.21.0 y npm 11.19.0, ya instalados.
- Angular CLI generador 22.2.6; CLI local resuelto 22.2.7.
- Angular 22.2.6; TypeScript 6.0.3; Bootstrap 5.3.
- Compatibilidad: https://angular.dev/reference/versions (Node ^24.15.0 admitido).

## Verificaciones realizadas

`npm test -- --watch=false`: salida real resumida:

```text
RUN v5.1.0
Test Files  1 passed (1)
Tests       2 passed (2)
Duration    4.17s
```

Se actualizó la prueba de título generada por el CLI a Academia UPN.

`npm run build`: salida real resumida:

```text
styles-2VG5FPJR.css   231.64 kB
main-6TH6XKEK.js      189.85 kB
Initial total        421.49 kB
Application bundle generation complete. [5.762 seconds]
Output location: dist/academia-web
```

La compilación se abortó dos veces con código 134 en el sandbox, sin diagnóstico de código. La misma compilación terminó con código 0 fuera del aislamiento. Las pruebas pasaron dentro del sandbox.

## Adaptaciones y estado

No se reemplazó Node ni se instaló un CLI global. Se usa el CLI local mediante npm. Se configuró Bootstrap, los colores de la guía, idioma español y RouterOutlet. El entorno de desarrollo apunta a localhost:8080/api/v1; producción usa /api/v1 hasta configurar el despliegue del Módulo IV.

La API no estaba accesible en localhost:8080 en la comprobación inicial. No se ha realizado aún una prueba de integración ni revisión visual del navegador. Eso corresponde a las siguientes partes; esta etapa acredita la estructura, las pruebas iniciales y la compilación.

El backend y sus commits permanecen sin cambios. El frontend tiene un repositorio local independiente, rama laboratorio-3, sin publicación remota.
