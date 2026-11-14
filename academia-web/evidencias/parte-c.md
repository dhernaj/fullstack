# Parte C — navegación, roles y página 404

Fecha local: 12 de noviembre de 2026. Pasos 13–17 de la guía.

## Implementación

- Directiva estructural appHasRole con un rol o varios; actualiza el contenido al cambiar los permisos.
- Barra de navegación con enlaces Cursos, Nuevo curso solo para ADMIN, usuario, rol y botón Salir.
- El menú admite varias líneas en pantallas estrechas y utiliza una etiqueta accesible para la navegación.
- Página 404 con carga diferida. Sin sesión permite ir al login; con sesión ofrece volver a cursos.
- Guard funcional: sin sesión redirige al login conservando la URL en volver; sin el rol requerido redirige a cursos.
- Componente raíz actualizado para incluir la barra y el área de rutas.

## Adaptación observada en la API real

La sesión del administrador incluía ADMIN y una autoridad interna FACTOR_PASSWORD de Spring. El frontend conserva la respuesta de sesión, pero expone como roles de la aplicación solamente ADMIN, DOCENTE y ESTUDIANTE. La prueba de sesión comprueba este caso. No se cambió el backend.

## Verificaciones automáticas

`npm test -- --watch=false`, código de salida 0:

```text
Test Files  5 passed (5)
Tests       35 passed (35)
Start at    21:54:23
Duration    7.79s
```

Las 15 pruebas nuevas cubren menú sin sesión, ADMIN, DOCENTE, ESTUDIANTE, actualización de la directiva y cierre de sesión; también redirección con URL de regreso, listado para los tres roles, formulario permitido a ADMIN y rechazado a DOCENTE/ESTUDIANTE, y enlaces de regreso del 404.

Para probar el guard se utilizaron RouterTestingHarness y pantallas de prueba de listado/formulario. Las rutas reales de cursos aún no existen: se registrarán junto con sus canActivate en las partes D y E, como indica la guía. Hasta entonces, los enlaces de cursos llegan al 404. No se presenta esta prueba como una verificación del listado real.

`npm run build`, fuera del sandbox, código de salida 0:

```text
Initial total: 491.68 kB
Lazy chunk login: 44.96 kB
Lazy chunk not-found: 797 bytes
Application bundle generation complete. [8.490 seconds]
```

Sin errores ni advertencias de Angular. `git diff --check` también pasó.

## Comprobaciones en el navegador

API local del Laboratorio 2, perfil H2 de demostración, sin modificar PostgreSQL.

1. Sesión admin: se mostraron Cursos, Nuevo curso, admin · ADMIN y Salir. Captura: menu-admin.png.
2. Al pulsar Salir desaparecieron los enlaces de sesión y quedó la marca con enlace al login. Captura: menu-sin-sesion.png.
3. Se inició sesión con la cuenta estudiante y su clave local existente. Se mostraron Cursos, estudiante · ESTUDIANTE y Salir, sin Nuevo curso. Captura: menu-estudiante.png.
4. La ruta /xyz mostró 404 y Volver a cursos. Captura: pagina-404.png.

Las capturas no incluyen contraseñas ni tokens. La pestaña quedó en /login con la sesión del estudiante. El código del backend y sus commits permanecen sin cambios.

Siguiente etapa: parte D, pasos 18–20, servicio y listado de cursos con búsqueda y paginación.
