# Academia UPN API de cursos

## Módulo IV — configuración de esta copia

En el monorepo, desde la parte B, la ejecución normal usa PostgreSQL y H2 queda con alcance test. `./mvnw verify` usa `src/test/resources/application.properties`, una base H2 en memoria y una clave JWT pública exclusiva de pruebas; no requiere `.env.local`. JaCoCo genera `target/site/jacoco/index.html` y exige al menos 50 % de líneas cubiertas. No se excluyen clases de negocio del análisis.

La parte D incorpora Swagger en /swagger-ui.html, el contrato /v3/api-docs y el health check público /actuator/health. El lanzador de documentación de la raíz permite una vista H2 de demostración local; consultar el README del monorepo.

Las secciones siguientes conservan el registro del Laboratorio 2 de origen; sus referencias al perfil H2 de desarrollo ya no describen la configuración de esta copia del Módulo IV. Los proyectos originales no se modificaron.

API REST del laboratorio con Java 17, Spring Boot 4.1.2, Spring Data JPA, Hibernate y PostgreSQL 17. Los datos se conservan entre reinicios. La guía utilizada para esta versión es Modulo1_Laboratorio.docx, adaptada a macOS Intel.


## Laboratorio 2 — avance actual

La rama laboratorio-2 continúa el Laboratorio 1 con persistencia y seguridad. En esta etapa se añadieron H2, Spring Security y el resource server OAuth2. La configuración JWT y CORS está implementada. La parte B añade las entidades Estudiante, Matricula y Usuario, los enums Rol y EstadoMatricula, las relaciones y la versión optimista de Curso. La parte E incorpora login, emisión y validación JWT y permisos por rol.

H2 es el perfil predeterminado: crea su base en memoria y la descarta al terminar. PostgreSQL mantiene academia_db y ddl-auto=update, sin recrear las tablas existentes. Ambos perfiles desactivan open-in-view. Se mantuvieron Java 17 y Spring Boot 4.1.2. El límite de página se configuró en 50; el listado se adaptará a Pageable en la parte C.

```sh
# Desarrollo con H2
./iniciar-macos.command

# Base persistente existente
SPRING_PROFILES_ACTIVE=postgres ./iniciar-macos.command
```

Cada petición protegida requiere Authorization: Bearer <token>. POST /api/v1/auth/login recibe username y password. El servidor usa sesiones STATELESS y no inicia sesión con Basic o formularios. Los reportes del Laboratorio 1 y las pruebas Basic temporales de las partes anteriores se conservan como evidencia histórica.

JWT_SECRET se genera localmente y permanece en .env.local (fuera de Git). No se incluye el secreto de ejemplo de la guía. Para configurar una nueva máquina, generar al menos 32 bytes aleatorios en Base64 y exportarlos como JWT_SECRET. Las pruebas de esta etapa se guardan en evidencias/laboratorio-2/parte-a.

El modelo se verifica con pruebas de persistencia en H2 y PostgreSQL; evidencias en evidencias/laboratorio-2/parte-b. La parte C añade repositorios, consultas y servicios, con paginación y reglas de matrícula. La parte D amplía los errores de autenticación, acceso y concurrencia. La parte F documenta los controles verificados y los pendientes. El reto también está verificado en H2 y PostgreSQL; queda preparar el informe final de entrega. El resto de este README documenta la base funcional del Laboratorio 1.

## Ejecutar en esta Mac

```sh
./iniciar-macos.command
```

El script inicia PostgreSQL local si está detenido, carga .env.local y ejecuta Spring Boot. La API está en http://127.0.0.1:8080/api/v1/cursos. La ruta raíz no tiene controlador.

Base: academia_db; usuario: academia_app. La contraseña está en .env.local, excluido de Git. El script presupone que PostgreSQL y la base ya están configurados en esta Mac; no crea usuarios ni restaura respaldos.

## Arquitectura

CursoController recibe las peticiones y delega en CursoService. El servicio concentra reglas y transacciones, y utiliza CursoRepository, una interfaz JpaRepository. Curso es la entidad; CursoRequest, CuposRequest y CursoResponse definen los datos de entrada y salida. GlobalExceptionHandler produce errores en formato Problem Details.

La clase principal está en pe.edu.upn.academia_api; el escaneo explícito de componentes, entidades y repositorios incluye los paquetes de pe.edu.upn.academia.

DataLoader inserta SIS-101, SIS-205 y SIS-310 solo cuando la tabla está vacía. Hibernate usa ddl-auto=update para el desarrollo. La tabla se creó desde cero mediante Hibernate en el paso 7 y se guardó su DDL como evidencia.

## Endpoints

| Método | Ruta | Resultado |
|---|---|---|
| GET | /api/v1/cursos | 200, cursos activos; filtros docente y creditosMin |
| GET | /api/v1/cursos/{id} | 200, incluso si está inactivo; 404 si no existe |
| POST | /api/v1/cursos | 201 y Location; 400 por validación; 409 por código duplicado |
| PUT | /api/v1/cursos/{id} | 200; 400 por validación; 404 inexistente; 409 código ocupado |
| PATCH | /api/v1/cursos/{id}/cupos | 200; cupos entre 1 y 60; 400 inválido; 404 inexistente |
| DELETE | /api/v1/cursos/{id} | 204 y activo=false; repetición 204; id inexistente 404 |

POST y PUT reciben codigo, nombre, creditos, docente y cupos. PATCH recibe solo cupos. Las entradas se validan con @Valid; id y activo no forman parte del DTO de entrada. Los errores de validación incluyen mensajes por campo.

## Pruebas y evidencias

```sh
source ./.env.local
./mvnw clean verify
```

Las pruebas Java requieren PostgreSQL iniciado. Las pruebas transaccionales revierten los cambios, pero las secuencias pueden avanzar. Los ids no tienen por qué comenzar en 1 ni ser consecutivos.

- Última ejecución completa de pruebas Java: 19 pruebas aprobadas, en evidencias/paso-14.
- Colección de la sección 5: 14 casos de guía, desglosados y ampliados hasta 35 peticiones y 57 aserciones, sin fallos. Se ejecutó con Newman 6.2.1; reportes en evidencias/pruebas-completas.
- Colección importable: postman/academia-guia2-pruebas.postman_collection.json. Instrucciones y repetición: postman/LEEME-GUIA2.md.
- Revisión de seguridad y sus comprobaciones: SEGURIDAD.md y evidencias/seguridad.

Los archivos academia-upn-modulo1.* en postman y los commits iniciales se conservan como historial del laboratorio anterior. La colección y los reportes con nombre guia2 corresponden a esta versión.

Las pruebas HTTP crean cursos y los conservan inactivos al terminar. El listado muestra solo los tres cursos de ejemplo activos. Los cinco casos del reto están documentados en evidencias/paso-15: 10 peticiones y 15 aserciones Newman aprobadas, con los datos originales conservados.

## Seguridad

La revisión de los ocho hallazgos de la guía, su evidencia y pendientes está en [SEGURIDAD.md](SEGURIDAD.md). La API utiliza DTO, validación, repositorios JPA con parámetros y errores sin trazas en la respuesta. El rol PostgreSQL no es superusuario; .env.local está excluido de Git.

La rama laboratorio-2 incorpora autenticación JWT y autorización por rol y propiedad del estudiante. El servicio escucha solo en la interfaz local. No se ha realizado una auditoría integral ni un escaneo de vulnerabilidades de dependencias.

## Avance y commits

GUIA2.md registra los pasos y las adaptaciones. Cada paso verificado tiene un commit separado a partir de la preparación de la segunda guía. El código original y su ZIP permanecen en Descargas; las copias y los respaldos están documentados en esta carpeta de trabajo.

## Referencias

- [Spring Boot y bases SQL](https://docs.spring.io/spring-boot/reference/data/sql.html)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa/)
- [OWASP Top 10:2025](https://top10.owasp.org/2025/)
- [OWASP API Security Top 10:2023](https://api-security.owasp.org/editions/2023/en/0x11-t10/)
- [Newman](https://learning.postman.com/docs/reference/newman-cli/installing-running-newman)

## Modelo de datos del Laboratorio 2

Matricula relaciona un Estudiante con un Curso mediante dos @ManyToOne LAZY y guarda fecha y estado. Una restricción única impide repetir estudiante y curso. Las listas de Curso y Estudiante usan mappedBy; no se usa @ManyToMany porque la matrícula tiene datos propios.

Usuario almacena username, hash de contraseña y rol, y puede vincularse con un único estudiante mediante @OneToOne. La codificación de contraseñas de las cuentas reales se incorporará en la parte de seguridad; la entidad por sí sola no convierte texto plano en hash.

Curso incorpora @Version para detectar ediciones con una versión obsoleta. Al actualizar la base del Laboratorio 1, ejecutar db/laboratorio2/01_inicializar_version.sql antes del primer arranque de esta versión. Ese script añade version si falta e inicializa las filas existentes a cero. En esta Mac ya se aplicó, con respaldo previo y verificación de los datos. No se necesita para H2 nuevo.

El bloqueo para evitar dos matrículas en el último cupo pertenece a las partes de servicio y concurrencia; @Version en Curso no lo resuelve por sí solo.

## Consultas y servicios del Laboratorio 2

El listado GET /api/v1/cursos ahora devuelve PaginaResponse: contenido, pagina, tamanio, totalElementos, totalPaginas y ultima. Acepta page, size y sort además de docente y creditosMin. La página comienza en 0 y el tamaño máximo es 50. El filtro de activos se aplica en la base, y los caracteres % y _ del filtro docente se tratan de forma literal mediante la consulta derivada. Esta respuesta cambia el contrato del Laboratorio 1, que devolvía una lista simple.

EstudianteService permite crear, obtener y listar estudiantes, con comprobación de código y correo repetidos. MatriculaService exige estudiante y curso existentes, curso activo, matrícula única y plazas disponibles. El retiro conserva la matrícula y libera la plaza; no permite volver a matricular al mismo estudiante en ese curso porque la combinación permanece única.

La matrícula bloquea la fila del curso con PESSIMISTIC_WRITE mientras cuenta plazas y guarda el registro. La prueba real de dos solicitudes simultáneas quedó aprobada en el reto, con evidencias en H2 y PostgreSQL. CursoService rechaza eliminar un curso si tiene matrículas, según la tabla de pruebas del segundo laboratorio.

Las consultas de detalle usan JOIN FETCH y los DTO se construyen dentro de la transacción. La comparación con dos matrículas arrojó 5 consultas sin fetch y 2 con fetch. El resumen de ocupación usa una proyección JPQL y cuenta solo matrículas activas, incluyendo los cursos sin matrículas con total cero.

La parte E publica los servicios de estudiantes y matrículas en sus controladores, con permisos por rol y propiedad del registro. Las evidencias de la parte C siguen describiendo las pruebas de servicio y repositorio realizadas en aquella etapa. Evidencias: evidencias/laboratorio-2/parte-c.

## Excepciones del Laboratorio 2

GlobalExceptionHandler mantiene Problem Details y los errores por campo. Ahora distingue AuthenticationException (401 y mensaje genérico), AccessDeniedException (403), ObjectOptimisticLockingFailureException (409), además de los conflictos de negocio e integridad. Los errores inesperados siguen respondiendo 500 sin detalles internos.

Las 49 pruebas de H2 incluyen ocho pruebas MVC de resolución de excepciones. El controlador que provoca errores solo existe en src/test y no se empaqueta. La parte E añade las pruebas reales del filtro JWT y de los permisos con usuarios de demostración. Los errores ocurridos antes del controlador requieren los manejadores de la cadena de seguridad. Evidencias: evidencias/laboratorio-2/parte-d.

## Inicio de sesión y permisos del Laboratorio 2

El script local habilita los datos de demostración mediante APP_DEMO_ENABLED=true. Crea admin, docente y estudiante con roles ADMIN, DOCENTE y ESTUDIANTE, y dos estudiantes ficticios. La cuenta estudiante está vinculada a N00123456, con dos matrículas iniciales. Las contraseñas se generan localmente y están en las variables LAB_ADMIN_PASSWORD, LAB_DOCENTE_PASSWORD y LAB_ESTUDIANTE_PASSWORD de .env.local; en la base se guardan hashes BCrypt. No se imprimen ni se incluyen en Git.

Fuera del script, los datos demo están deshabilitados por defecto. Para impedir su carga desde el script, usar APP_DEMO_ENABLED=false. Si una cuenta ya existe, el cargador no cambia su contraseña ni rol. En PostgreSQL la carga demo es persistente; habilitarla solo cuando se desee añadir esos datos del laboratorio.

| Operación | Permiso |
|---|---|
| POST /api/v1/auth/login | Público, credenciales válidas para obtener token |
| GET /api/v1/cursos | Cualquier usuario autenticado |
| Crear, actualizar, PATCH o eliminar cursos | ADMIN |
| Matrículas de un curso y resumen de ocupación | ADMIN o DOCENTE |
| Listar, consultar o crear estudiantes | ADMIN o DOCENTE |
| Matrículas de un estudiante | ADMIN o DOCENTE; ESTUDIANTE solo las propias |
| POST /api/v1/matriculas | ADMIN o DOCENTE |
| PATCH /api/v1/matriculas/{id}/retiro | ADMIN |

Los tokens HS256 duran 60 minutos, usan el emisor academia-api y contienen usuario, roles y fechas, sin contraseña ni datos personales del estudiante. El decodificador exige una clave de al menos 32 bytes, valida firma, vencimiento y emisor. CORS permite http://localhost:4200 y expone Location.

Los errores de los filtros devuelven Problem Details; un 401 incluye WWW-Authenticate: Bearer. CORS rechaza un origen desconocido con 403 del framework. La colección importable y sus instrucciones están en postman/academia-lab2-seguridad.postman_collection.json y postman/LEEME-LABORATORIO2.md. Las evidencias depuradas de contraseñas y tokens están en evidencias/laboratorio-2/parte-e.

## Revisión de seguridad del Laboratorio 2

La parte F documenta seis hallazgos de la guía y la matriz de permisos en SEGURIDAD.md. Pasaron 21 peticiones de revisión, sin modificar los datos. Siguen pendientes el límite de intentos de login y la revocación de tokens. La verificación de dos matrículas simultáneas para el último cupo quedó aprobada después en el reto. Evidencias: evidencias/laboratorio-2/parte-f.

## Reto del Laboratorio 2 completado

Los cinco casos se verificaron con H2 y PostgreSQL: acceso a matrículas propias, rechazo de acceso ajeno, permiso del docente, página máxima de 50 y dos peticiones simultáneas al último cupo. Se ejecutaron tres carreras por perfil; todas terminaron en 201/409 y una sola matrícula activa por curso. PostgreSQL se probó en un esquema temporal aislado y eliminado al finalizar, con las tablas públicas conservadas.

Evidencias y repetición: evidencias/laboratorio-2/reto/RESULTADOS.md. La prueba de concurrencia usó Python y solicitudes HTTP reales; no fue una ejecución secuencial de la colección Postman. No se modificó código funcional en esta etapa.
