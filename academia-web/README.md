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

Se mantendrá un commit por parte A–F. Las partes A–F están completadas. La integración final se verificó con PostgreSQL, incluida la persistencia después de reiniciar la API.

## Parte B — pasos 7 a 12

Login con validación de campos, sesión en sessionStorage, vencimiento automático e interceptor funcional. El token se adjunta solo al origen y la ruta de nuestra API; un 401 de un recurso protegido cierra la sesión. Un 403 conserva la sesión para que el usuario pueda seguir usando sus permisos disponibles.

Se usan los nombres de la API existente: accessToken, tokenType, expiresIn, username y roles. No es necesario modificar el backend para seguir el ejemplo de la guía.

La parte B utilizó una confirmación temporal después del login. Desde la parte D, el ingreso correcto navega a /cursos o a una ruta interna indicada en volver.

Las cuentas de demostración siguen usando las claves locales del Laboratorio 2. No se incluyen en este repositorio. Para iniciar la API, abrir otra terminal en academia-api-guia2 y ejecutar `./iniciar-macos.command`; ese lanzador conserva su perfil predeterminado H2. Para la integración final de este módulo se usa el lanzador PostgreSQL descrito al final de este README.

La comprobación HTTP puede repetirse con la API encendida y LAB_ADMIN_PASSWORD cargada en el entorno:

```sh
python3 scripts/verificar-api.py
```

Las evidencias de las partes A y B están en `evidencias/`. La parte B se verificó inicialmente con H2; la revisión final se repitió con PostgreSQL en la parte F.

## Parte C — pasos 13 a 17

Barra de navegación con usuario y rol, opción Nuevo curso exclusiva de ADMIN y botón Salir. La directiva appHasRole admite un rol o varios y reacciona a los cambios de sesión. El menú muestra solo los roles académicos; las autoridades internas que también envía Spring no se muestran como roles de la aplicación.

La página 404 se carga bajo demanda para rutas desconocidas. Permite volver a cursos si hay sesión o al login en caso contrario.

El guard authGuard conserva la URL solicitada en el parámetro volver cuando falta la sesión. Si la ruta exige roles y el usuario no los tiene, redirige a /cursos. Se comprobó con RouterTestingHarness. Desde la parte D el listado real /cursos usa canActivate: [authGuard]; desde la parte E las rutas del formulario también exigen el rol ADMIN.

Las comprobaciones de menú se repitieron en navegador con admin y estudiante, además de la salida de sesión y la ruta /xyz. Capturas y resultados en evidencias/parte-c.md.

## Parte D — pasos 18 a 20

Servicio tipado de cursos y listado paginado con búsqueda por docente, espera de 400 ms, descarte de búsquedas duplicadas y cancelación de consultas anteriores. Las suscripciones se liberan al abandonar la pantalla. El listado muestra carga, ausencia de resultados y un botón para reintentar si falla la consulta.

Se conserva el tamaño inicial de cinco cursos. El selector permite dos, cinco o diez para probar la navegación con los tres cursos de demostración sin modificar datos. Los botones Anterior y Siguiente quedan deshabilitados en los límites.

El administrador dispone de Editar y Eliminar. Eliminar pide confirmación, evita envíos simultáneos, muestra el detalle de errores Problem Details y retrocede de página si se borra su último elemento. El backend sigue siendo responsable de autorizar las operaciones. Durante la parte D, Nuevo curso y Editar llegaban al 404; desde la parte E abren el formulario.

La ruta inicial redirige a cursos. Sin sesión, el guard lleva a /login conservando volver; al ingresar regresa al listado. La revisión en navegador se realizó con H2, admin y estudiante. Se verificaron búsqueda y paginación real sin crear ni borrar cursos; el borrado y sus errores se probaron con HTTP simulado. Resultados y capturas en evidencias/parte-d.md.

El presupuesto de advertencia del paquete inicial se ajustó a 550 kB en la parte D; el límite de error permanece en 1 MB. El tamaño observado fue 500.22 kB.

## Parte E — pasos 21 a 22

Formulario compartido para crear y editar, con validaciones equivalentes a CursoRequest: código AAA-999, nombre de hasta 100 caracteres, docente de hasta 80, créditos enteros entre 1 y 6 y cupos enteros entre 1 y 60. Los textos vacíos o compuestos solo por espacios se rechazan.

Las rutas /cursos/nuevo y /cursos/:id/editar usan authGuard y data.roles ADMIN. La edición carga el curso por id; un id inválido, un 404 o un fallo de lectura impiden guardar un formulario sin datos. Cambiar el id cancela la lectura anterior. El guardado evita el doble envío y muestra errores 400 por campo y el detalle de conflictos 409.

Se corrigió el regreso del login cuando Angular enlaza volver como undefined: el destino por defecto sigue siendo /cursos.

Se verificó en el navegador la creación de SIS-500, el rechazo de un duplicado y su edición a Pruebas de Software II. Solo se modificó este curso nuevo en H2; los tres cursos iniciales permanecieron intactos. El registro es de demostración y no sobrevivirá a un reinicio de la base H2 en memoria. La comprobación SQL pendiente de esta etapa se completó en la parte F, utilizando un esquema PostgreSQL propio del Laboratorio 3.

Capturas y detalle en evidencias/parte-e.md. Las pruebas de esta etapa terminaron con 90 casos; la revisión final amplió el total a 93.

## Parte F — pasos 23 a 25 y persistencia final

93 pruebas aprobadas, build sin advertencias, 25 comprobaciones HTTP de seguridad y npm audit sin vulnerabilidades reportadas. Las decisiones de revisión están en PROMPTS.md y los controles y límites en SEGURIDAD.md.

La integración final usa academia_db, esquema laboratorio3_angular. Se respaldó public antes de empezar; las filas y huellas de cursos, estudiantes, matrículas y usuarios de ese esquema permanecieron iguales. Los 14 cursos anteriores no se sobrescribieron.

En el nuevo esquema se crearon los datos de demostración del backend existente y el curso SIS-500 desde Angular. Se verificaron el conflicto por duplicado y la edición a Pruebas de Software II. La consulta SQL y el navegador confirmaron el registro después de reiniciar la API. El código del backend no se modificó.

### Reanudar en esta Mac Intel

1. Iniciar PostgreSQL 17 instalado en /usr/local/opt/postgresql@17, si está detenido.
2. Mantener academia-api-guia2 como carpeta hermana de academia-web, con su .env.local privado y su JAR del Laboratorio 2 ya construido. Si falta el JAR, ejecutar ./mvnw package dentro del backend.
3. Confirmar que el puerto 8080 está libre. Ejecutar en academia-web:

```sh
./scripts/iniciar-api-postgres.command
```

En otra terminal:

```sh
npm ci
npm start -- --host localhost
```

Abrir http://localhost:4200. Las cuentas admin, docente y estudiante usan las contraseñas locales ya configuradas en el backend; no están en esta entrega.

El esquema laboratorio3_angular ya existe en esta Mac. En una instalación nueva, crear el esquema una sola vez usando scripts/postgres/preparar.sql y la conexión privada del backend, antes de iniciar la API. Ese SQL falla si el esquema ya existe y no borra nada. El lanzador presupone PostgreSQL iniciado, la base existente y las credenciales del Laboratorio 2.

Para repetir la comprobación de seguridad, cargar .env.local del backend en el entorno de la terminal y ejecutar python3 scripts/verificar-seguridad.py. La consulta de persistencia está en scripts/postgres/curso-evidencia.sql.

El índice de entrega está en evidencias/INDICE.md. Hay un commit por parte A–F. El reto adicional de estudiantes, orden por cabeceras y Angular Material es opcional y no se implementó. El despliegue corresponde al siguiente módulo.
