# Laboratorio 2 — parte E

Se implementaron Spring Security, autenticación de cuentas con BCrypt, emisión y validación JWT HS256, autorización por roles y CORS. Se publicaron los controladores de estudiantes y matrículas y las consultas restringidas de cursos.

## Configuración

La API usa STATELESS, sin autenticación por cookies; el token se envía en Authorization: Bearer. Se desactiva CSRF para ese esquema y no se guarda la petición para una sesión posterior. Todos los accesos están restringidos, excepto login y el manejo de errores. Las rutas no definidas se deniegan por defecto.

El secreto JWT se exige por variable local, sin la clave de ejemplo de la guía, y se comprueba que tenga al menos 32 bytes decodificados. El token contiene iss, sub, iat, exp y roles. Se validan emisor, firma HS256 y vencimiento. Los roles del claim se convierten a autoridades ROLE_ADMIN, ROLE_DOCENTE y ROLE_ESTUDIANTE.

Los errores del filtro se responden con Problem Details; 401 incluye WWW-Authenticate: Bearer. Los errores del controlador siguen el manejador de la parte D. El preflight CORS permite http://localhost:4200 y rechaza otros orígenes con el 403 de Spring.

La restricción de matrículas propias usa la relación Usuario–Estudiante en @PreAuthorize. Se incorporó junto con el controlador de estudiante, como requiere el código del documento; su verificación final se repetirá en el reto.

## Datos de demostración

DemoDataLoader es opcional y está deshabilitado por defecto. El script local lo habilita para el laboratorio. Crea cuentas admin, docente y estudiante con contraseñas locales y hashes BCrypt, dos estudiantes ficticios y dos matrículas del primer estudiante. No cambia cuentas ya existentes.

Las pruebas Java no habilitaron el cargador demo. La colección HTTP se ejecutó en un H2 nuevo y descartable. No se añadieron cuentas o matrículas demo a PostgreSQL en esta etapa; se comparó el catálogo persistente antes y después y se conservó completo.

## Resultados

| Caso de la guía | Esperado | Obtenido |
|---|---|---|
| 1. GET cursos sin token | 401 y challenge Bearer | Aprobado |
| 2. Login con contraseña incorrecta | 401 genérico | Aprobado |
| 3. Página ordenada como estudiante | 200, 3 cursos, SIS-310 primero | Aprobado |
| 4. Crear curso como estudiante | 403 | Aprobado |
| 5. Crear curso como admin | 201 | Aprobado |
| 6. Matrículas de curso como estudiante | 403 | Aprobado |
| 7. Matrícula en curso sin cupos como docente | 409 | Aprobado |
| 8. Matrícula duplicada como admin | 409 | Aprobado |
| 9. Retiro de matrícula como docente | 403 | Aprobado |
| 10. Eliminar curso con matrículas como admin | 409 | Aprobado |
| 11. Preflight de origen permitido | 200 y Allow-Origin correcto | Aprobado |
| 12. Preflight de origen desconocido | 403 | Aprobado |

Pasaron 52 pruebas Java con H2 y 52 con PostgreSQL, incluidas tres pruebas nuevas de contenido del token, vencimiento y emisor incorrecto. La colección de Postman, ejecutada mediante Newman, aprobó 28 peticiones y 39 aserciones. No se observaron cookies de sesión en esas respuestas.

Las comprobaciones adicionales cubrieron usuario desconocido, token mal formado o con firma alterada, login vacío, acceso a matrícula propia y ajena, retiro por ADMIN, uso de la plaza liberada y acceso al resumen por rol. Se usó un curso temporal de una plaza para comprobar el curso lleno sin alterar los ejemplos.

Se corrigió una aserción de preparación: después de añadir una matrícula al primer estudiante, su consulta propia devolvía tres (dos iniciales y una de prueba). La comprobación final verifica esas tres y que todas pertenezcan al estudiante autenticado. No fue necesario cambiar la autorización.

## Evidencias y alcance

- pruebas-h2.txt y pruebas-postgres.txt: batería completa.
- ejecucion-newman.txt, resultado-newman.json y resumen.json: colección HTTP. El JSON conserva el formato de Newman con contraseñas y tokens omitidos, incluidos los cuerpos codificados como Buffer.
- servidor-seguridad.txt: arranque del servidor H2 usado para probar.
- estado-postgresql.txt: cursos conservados y tablas nuevas sin registros de prueba persistentes.
- probar_seguridad.py y ejecutar-coleccion.cjs: repetición.

Las credenciales permanecen en .env.local, excluido de Git, y no se incluyeron tokens reutilizables en los reportes. No se enviaron tokens a jwt.io; su contenido se verificó localmente. Las pruebas HTTP por rol se realizaron con H2; PostgreSQL se verificó con la batería Java, sin cargar usuarios demo.

No se ejecutó todavía la prueba de dos matrículas HTTP simultáneas para el último cupo. El límite de intentos de login y la revocación de tokens tampoco se implementaron; se documentarán en la revisión de seguridad.

Siguiente: parte F, revisión de seguridad, y después los casos del reto.

Referencias: [JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) y [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/dao-authentication-provider.html).
