# Academia web — Laboratorio 3

Frontend Angular para la API de los laboratorios anteriores.

## Desarrollo en macOS

```sh
npm ci
npm start -- --host localhost
```

Abrir http://localhost:4200. La API del Laboratorio 2 debe ejecutarse en http://localhost:8080 para las partes B–F.

```sh
npm test -- --watch=false
npm run build
```

## Parte A — pasos 1 a 6

Proyecto standalone sin SSR, Bootstrap 5.3, estilos del curso, idioma español y componente inicial con RouterOutlet.

Se conserva Node.js 24.21.0 instalado en la Mac Intel: es compatible con Angular 22. El proyecto se generó con Angular CLI 22.2.6 y npm resolvió CLI 22.2.7 / Angular 22.2.6; se usa desde npm, sin instalación global. Las versiones resueltas quedan en package-lock.json.

El entorno de desarrollo apunta a http://localhost:8080/api/v1. Producción usa /api/v1 como ruta del mismo dominio hasta configurar el despliegue en el Módulo IV; no se utiliza el dominio de ejemplo de la guía como si estuviera desplegado. Los entornos no contienen contraseñas ni secretos JWT.

Se mantendrá un commit por parte A–F. Las partes A, B y C están completadas; sigue la parte D con el listado de cursos.

## Parte B — pasos 7 a 12

Login con validación de campos, sesión en sessionStorage, vencimiento automático e interceptor funcional. El token se adjunta solo al origen y la ruta de nuestra API; un 401 de un recurso protegido cierra la sesión. Un 403 conserva la sesión para que el usuario pueda seguir usando sus permisos disponibles.

Se usan los nombres de la API existente: accessToken, tokenType, expiresIn, username y roles. No es necesario modificar el backend para seguir el ejemplo de la guía.

Hasta construir /cursos en las partes siguientes, un ingreso válido muestra una confirmación en la pantalla de login. Así se puede comprobar esta etapa sin provocar el error de ruta inexistente que menciona la guía.

Las cuentas de demostración siguen usando las claves locales del Laboratorio 2. No se incluyen en este repositorio. Para iniciar la API, abrir otra terminal en academia-api-guia2 y ejecutar `./iniciar-macos.command`; el perfil local predeterminado es H2.

La comprobación HTTP puede repetirse con la API encendida y LAB_ADMIN_PASSWORD cargada en el entorno:

```sh
python3 scripts/verificar-api.py
```

Las evidencias de las partes A y B están en `evidencias/`. La integración de esta etapa se verificó con H2; no se ha ejecutado contra PostgreSQL en el Laboratorio 3.

## Parte C — pasos 13 a 17

Barra de navegación con usuario y rol, opción Nuevo curso exclusiva de ADMIN y botón Salir. La directiva appHasRole admite un rol o varios y reacciona a los cambios de sesión. El menú muestra solo los roles académicos; las autoridades internas que también envía Spring no se muestran como roles de la aplicación.

La página 404 se carga bajo demanda para rutas desconocidas. Permite volver a cursos si hay sesión o al login en caso contrario.

El guard authGuard conserva la URL solicitada en el parámetro volver cuando falta la sesión. Si la ruta exige roles y el usuario no los tiene, redirige a /cursos. Se comprobó con RouterTestingHarness y pantallas de prueba; los canActivate de las pantallas reales de cursos se registrarán al crearlas en las partes D y E, siguiendo la guía. Hasta entonces, los enlaces de cursos llegan al 404 porque esas pantallas aún no existen.

Las comprobaciones de menú se repitieron en navegador con admin y estudiante, además de la salida de sesión y la ruta /xyz. Capturas y resultados en evidencias/parte-c.md.
