# Parte B — sesión JWT y login

Fecha local: 12 de noviembre de 2026. Pasos 7–12 de la guía.

## Cambios

- Modelos TypeScript para cursos, paginación, respuesta JWT y Problem Details.
- AuthService con signals, roles, sessionStorage y vencimiento automático.
- Descarte de sesiones vencidas, incompletas o con JSON dañado.
- Interceptor funcional registrado con HttpClient; compara origen y límite de ruta antes de adjuntar Bearer. El login no recibe un token anterior.
- Un 401 de la API protegida limpia la sesión; un 403 no la elimina.
- Login con formulario reactivo, validaciones, prevención de doble envío, carga diferida y mensajes accesibles.

## Correspondencia con el backend

La guía presenta tipo, expiraEnSegundos y usuario. La API del Laboratorio 2 devuelve tokenType, expiresIn y username: se conservaron los nombres reales del backend. Pagina incluye también ultima, presente en la respuesta Java.

La guía advierte que /cursos no existe aún y que un login correcto provocará NG04002. En esta implementación se muestra una confirmación temporal hasta que la ruta esté creada. La contraseña se limpia después del ingreso; la confirmación desaparece si deja de haber sesión.

## Verificaciones

`npm test -- --watch=false`, código de salida 0:

```text
Test Files  3 passed (3)
Tests       20 passed (20)
Start at    21:47:26
Duration    7.72s
```

Cobertura de casos: login correcto e incorrecto, restauración y descarte de sesión, expiración, Bearer, origen externo, prefijo de ruta parecido, 401, 403, ruta inicial, campos vacíos, doble envío, desconexión y confirmación temporal.

`npm run build`, fuera del sandbox, código de salida 0:

```text
Initial total: 468.74 kB
Lazy chunk login: 44.95 kB
Application bundle generation complete. [7.389 seconds]
```

La salida confirma que el login se carga en un fragmento separado. Sin errores ni advertencias de Angular.

`python3 scripts/verificar-api.py`: 7 comprobaciones HTTP aprobadas, con la API real del Laboratorio 2 y perfil H2 de demostración. El detalle está en api-parte-b.json. El script usa la contraseña cargada en el entorno y no guarda contraseñas ni tokens.

## Comprobación en navegador

- http://localhost:4200 redirigió a /login y mostró el formulario.
- admin con clave incorrecta mostró “Usuario o contraseña incorrectos”. Captura: login-error.png.
- admin con su clave local existente inició sesión y mostró la confirmación. Captura: login-correcto.png.
- La contraseña quedó vacía después del ingreso correcto.

Las capturas se tomaron en el navegador real. No se muestran tokens ni claves. Los tests verificaron sessionStorage y el interceptor; no se inspeccionó manualmente DevTools.

Se inició el backend con su lanzador existente y perfil H2, y Angular en localhost:4200. No se modificó código ni configuración del backend, ni se alteró PostgreSQL. Falta la parte C: directiva por rol, navegación, página 404 y guard.

Referencias consultadas: https://angular.dev/guide/http/interceptors y https://angular.dev/guide/routing/route-guards.
