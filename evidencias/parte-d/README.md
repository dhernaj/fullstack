# Módulo IV — Parte D: OpenAPI y health check

Fecha local: 13 de noviembre de 2026. Pasos 14–17 de la guía.

## Implementación

Se añadieron springdoc-openapi-starter-webmvc-ui 3.2.0 y Spring Boot Actuator. La guía usa springdoc 3.0.3, pero su matriz oficial relaciona Boot 4.1.x con springdoc 3.1.x–3.2.x; se conservó Boot 4.1.2 y se eligió la versión estable compatible 3.2.0.

OpenApiConfig define el título Academia UPN API, versión de contrato 1.0.0, descripción y esquema HTTP Bearer JWT. Los cuatro controladores tienen sus tags. El login lleva un requisito de seguridad vacío para que el contrato no lo presente como un endpoint que exige un token previo.

CursoController documenta listado, filtros docente y creditosMin, parámetros page/size/sort, lectura por id, alta, edición, cupos, eliminación lógica y consultas de matrículas/ocupación. Se conservó la ordenación predeterminada del código existente. Las respuestas de error 400/404/409 declaran ProblemDetail y application/problem+json, evitando que Swagger infiera por error el esquema de un curso para esas respuestas.

Se hicieron públicas únicamente las rutas de Swagger/OpenAPI y /actuator/health, además del login y /error existentes. Se conservaron la denegación por defecto y los permisos especiales de cursos, resumen, matrículas, estudiantes y propiedad. Actuator expone solo health; show-details=never evita detalles de base de datos. En desarrollo Spring Boot puede añadir los nombres de grupos liveness/readiness al JSON, junto a status UP.

## Pruebas automatizadas

./mvnw -B verify, sin claves locales cargadas: 75 pruebas, cero fallos, cero errores y cero omitidas; BUILD SUCCESS. JaCoCo volvió a aprobar el mínimo y la cobertura actual de líneas es 87,16 %. Ver verify.txt y cobertura-resumen.json.

Se añadieron cinco casos a CursoApiIntegrationTest: health público sin detalles, Swagger HTML público, contrato con seguridad y paginación correctas, cierre del resto de Actuator y conservación del rechazo a ESTUDIANTE en resumen/matrículas de cursos. El caso del contrato también verifica que 400/409 apunten a ProblemDetail. Las pruebas anteriores siguen pasando.

La primera ejecución de esta parte pasó antes de afinar los esquemas de error. Tras observar el ejemplo incorrecto en Swagger se ajustó el contrato y se ejecutó de nuevo la suite, que pasó. verify.txt conserva la ejecución final; el log previo quedó fuera del repositorio en work/modulo4-respaldos.

## Comprobación real en Chrome

Se levantó una instancia separada en http://127.0.0.1:8083 con H2, perfil doc-preview y credenciales ficticias. El lanzador scripts/iniciar-documentacion.command usa spring-boot.run.useTestClasspath=true para disponer del driver H2 que no está en el JAR de producción. Fuerza loopback, configuración H2 y clave pública de demostración, sin leer .env.local ni conectar a PostgreSQL.

1. Swagger UI mostró las cuatro secciones y las operaciones existentes.
2. Se ejecutó GET cursos con Try it out sin token y se obtuvo 401. Captura: swagger-sin-token.png.
3. Se inició sesión desde POST auth/login con la cuenta ficticia admin y se aplicó el accessToken en Authorize. La interfaz mostró Authorized y los candados cerrados. Captura: swagger-autorizado.png, con el token enmascarado.
4. Se creó DOC-401 desde Swagger con nombre Documentación desde Swagger, 3 créditos y 20 cupos. La tabla de respuesta mostró 201 y Location. Se comprobó el resultado sin guardar el cURL con su encabezado Authorization ni el cuerpo de login con el JWT.
5. Se abrió /actuator/health sin token: status UP, sin detalles internos. Captura: health-up.png.
6. Después del ajuste final se recargó Swagger y se comprobó la presencia del esquema ProblemDetail.

Durante la recompilación, DevTools reinició la instancia H2 de demostración. Ese curso no se presenta como persistente: las capturas documentan la operación realizada en la sesión. No se modificó PostgreSQL ni los datos de los laboratorios anteriores.

## HTTP y contrato exportado

scripts/verificar-documentacion.py completó 12 comprobaciones en la instancia local: health, Swagger HTML, metadatos OpenAPI, Bearer, login público, tags, paginación y filtros, ProblemDetail, 401 sin JWT, login, POST 201/Location y cierre de /actuator/env. Creó DOC-402 únicamente en esa base H2 efímera.

http-resumen.json conserva los resultados y los datos ficticios del curso, sin contraseñas ni tokens. openapi.json contiene el contrato exportado, listo para importar en Postman. No se afirma que se haya hecho la importación manual en Postman.

## Revisión visual pendiente de A

Chrome abrió http://localhost:4201 y confirmó el formulario de login del Angular del monorepo, con redirección desde /cursos. Captura: monorepo-login.png. Esto completa la revisión visual que no pudo hacerse con el navegador integrado en A. No se hizo login desde ese frontend contra la instancia Swagger 8083.

## Estado

La instancia de documentación sigue en 127.0.0.1:8083 y la pestaña de Swagger quedó disponible en Chrome. Swagger no persiste el JWT al recargar; la vista final está sin autorización guardada. Los cambios de esta parte están solo en el monorepo del Módulo IV.

Sigue la parte E: Flyway, producción y Docker. La desactivación de Swagger en producción y las migraciones se implementarán allí.

Referencias: https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot y https://docs.spring.io/spring-boot/reference/actuator/endpoints.html.
