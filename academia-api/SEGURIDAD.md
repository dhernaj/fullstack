# Revisión de seguridad del Laboratorio 2

La parte F contrasta los seis hallazgos de la guía con el código de la rama laboratorio-2 y las respuestas de la API local. Se revisaron también las reglas de acceso a nivel de objeto y la ausencia de secretos en los archivos versionados actuales. No es una auditoría integral ni un análisis automático de dependencias.

## Hallazgos de la guía

| N.º | Hallazgo de referencia | Categoría orientativa | Estado y evidencia actual |
|---|---|---|---|
| 1 | Matrículas de un curso visibles para cualquier autenticado | API1 / API5, A01 | La URL y @PreAuthorize exigen ADMIN o DOCENTE. En la revisión, ESTUDIANTE recibió 403 y DOCENTE recibió 200. La consulta por estudiante permite al alumno solo sus propias matrículas; un id ajeno respondió 403. |
| 2 | Secreto JWT con valor por defecto en el código | A02 | No se copió el secreto de ejemplo. app.jwt.secret exige JWT_SECRET; la clave local tiene al menos 32 bytes decodificados y la configuración comprueba ese mínimo. .env.local está ignorado por Git y tiene permisos 600. Se verificaron los archivos versionados actuales, no todo el historial. |
| 3 | Login sin límite de intentos fallidos | A07 / API2 / API4 | Pendiente, como en la guía. El código no implementa límite de frecuencia ni bloqueo temporal. Las respuestas a usuario desconocido y contraseña incorrecta tienen el mismo mensaje, pero eso no reemplaza un control de intentos. No se realizó fuerza bruta ni prueba de carga. |
| 4 | Página sin límite de tamaño | API4 | Se configura max-page-size=50 y ambos servicios paginados aplican el máximo. GET cursos?size=100000 devolvió tamanio=50. Las pruebas de servicios de la parte C cubren también estudiantes. |
| 5 | Matrículas simultáneas ocupan el último cupo | A06 | MatriculaService usa PESSIMISTIC_WRITE sobre Curso dentro de la transacción antes de contar plazas e insertar. La regla se verificó después en el reto con seis carreras HTTP reales, tres por perfil. En la primera de PostgreSQL se observaron dos sesiones esperando el bloqueo; todas las carreras terminaron con 201/409 y una sola matrícula activa por curso. Evidencias en evidencias/laboratorio-2/reto. |
| 6 | CSRF deshabilitado | Depende del mecanismo de autenticación | La API usa Bearer explícito, STATELESS y no guarda solicitudes en sesión. Basic con credenciales válidas y una cookie que contenía un token válido recibieron 401. No se observaron cookies de sesión en las 21 respuestas. Si se introduce autenticación por cookies, debe revisarse esta decisión. Ser stateless por sí solo no garantiza protección CSRF. |

Las categorías se utilizan para orientar la revisión. La presencia de una configuración o de una medida no demuestra ausencia de toda vulnerabilidad de esa categoría.

## Matriz de permisos

La matriz procede de SecurityConfig y de las anotaciones de método. Las pruebas por rol de la parte E y las comprobaciones de esta revisión respaldan las rutas indicadas en sus reportes.

| Ruta y método | Acceso |
|---|---|
| POST /api/v1/auth/login | Público; credenciales válidas para emitir un token |
| GET /api/v1/cursos y /api/v1/cursos/{id} | Autenticado |
| POST, PUT, PATCH o DELETE de cursos | ADMIN |
| GET /api/v1/cursos/{id}/matriculas | ADMIN o DOCENTE |
| GET /api/v1/cursos/resumen | ADMIN o DOCENTE |
| GET o POST /api/v1/estudiantes y GET por id | ADMIN o DOCENTE |
| GET /api/v1/estudiantes/{id}/matriculas | ADMIN o DOCENTE; ESTUDIANTE solo si el id corresponde a su cuenta |
| POST /api/v1/matriculas | ADMIN o DOCENTE |
| PATCH /api/v1/matriculas/{id}/retiro | ADMIN |
| Rutas no definidas | Denegadas por defecto |

/error se permite para que el framework renderice errores; no es un endpoint de datos académicos.

La comprobación de propiedad usa el subject del JWT validado para consultar Usuario y su relación con Estudiante. No confía en un estudianteId elegido por el cliente dentro del token. Las rutas específicas se evalúan antes que los patrones generales.

## JWT, contraseñas y CORS

- La configuración admite HS256 y valida firma, vencimiento y emisor academia-api. La revisión HTTP rechazó token vencido, emisor incorrecto, alg=none y cambio de roles sin una firma válida, todos con 401.
- El payload contiene usuario, roles y fechas. Es legible y no está cifrado; no contiene contraseña ni datos personales del estudiante. Su contenido se verificó localmente en las pruebas de JWT, sin enviarlo a servicios externos.
- Las cuentas demo usan BCrypt. Sus contraseñas locales están fuera de Git y los reportes de la parte E omiten tanto claves como JWT reutilizables. Los datos demo están deshabilitados por defecto en la aplicación y el script local los habilita expresamente.
- CORS permite http://localhost:4200. El preflight de ese origen respondió 200; un origen distinto respondió 403. CORS no sustituye los permisos del servidor.
- Los errores del filtro y del controlador evitan exponer trazas al cliente; las comprobaciones de esta revisión revisaron las respuestas de rechazo. Los detalles internos solo se registran del lado del servidor.

## Pendientes y límites

El límite de intentos de login sigue pendiente. También falta un mecanismo de revocación: un token válido conserva sus claims hasta vencer aunque se cambie la contraseña o el rol en la base. La duración actual es de 60 minutos. Estas limitaciones deben considerarse antes de una exposición fuera del laboratorio local.

La prueba simultánea del último cupo quedó aprobada en el reto con H2 y PostgreSQL. Esa prueba acotada no sustituye un análisis de carga o de todas las interacciones concurrentes. @Version protege contra ediciones obsoletas de Curso, pero no reemplaza el bloqueo de matrícula porque insertar una matrícula no modifica necesariamente la fila del curso.

No se ejecutaron un análisis de dependencias, un análisis de todo el historial Git, una restauración de respaldo ni pruebas de carga. La configuración sigue siendo de desarrollo: H2 en memoria o PostgreSQL con ddl-auto=update, escucha local y datos ficticios. Este informe no certifica que el servicio esté preparado para producción.

## Evidencias

- evidencias/laboratorio-2/parte-f/resultados.json: 21 peticiones, revisión de secretos locales y catálogo persistente conservado.
- evidencias/laboratorio-2/parte-f/verificar_seguridad.py: ejecutor de las comprobaciones locales, sin escrituras en registros.
- evidencias/laboratorio-2/parte-e: 52 pruebas por perfil y 28 peticiones por rol, JWT y CORS.
- evidencias/laboratorio-2/parte-c: medición de consultas y reglas de plazas a nivel de servicio.
- evidencias/laboratorio-2/reto: cinco casos en ambos perfiles, doce solicitudes concurrentes y observación del bloqueo en PostgreSQL.

Los tokens y las contraseñas no se guardan en las evidencias. Los tokens de prueba vencidos o de otro emisor se firmaron localmente con la clave del entorno para comprobar la validación, y tampoco se incluyeron en el reporte.

La revisión del Laboratorio 1 permanece en el historial de main y en sus evidencias anteriores. Este archivo describe el estado actual del Laboratorio 2.

## Referencias

- [OWASP API Security Top 10:2023](https://api-security.owasp.org/editions/2023/en/0x11-t10/)
- [Autorización a nivel de objeto — API1](https://github.com/OWASP/API-Security/blob/master/editions/2023/en/0xa1-broken-object-level-authorization.md)
- [Autorización de funciones — API5](https://github.com/OWASP/API-Security/blob/master/editions/2023/en/0xa5-broken-function-level-authorization.md)
- [CSRF y aplicaciones sin estado — Spring Security](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html)
