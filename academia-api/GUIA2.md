# Guía 2 — avance por pasos

Esta copia conserva los commits del proyecto original del Módulo I. El commit de preparación retira temporalmente las capas adelantadas para seguir la segunda guía desde el paso 7. Las versiones completas anteriores permanecen en sus carpetas y respaldos.

Base: academia_db; usuario: academia_app; credenciales locales en .env.local, excluido de Git. Se conserva Java 17 y Spring Boot 4.1.2. PostgreSQL está instalado con Homebrew en macOS Intel; no se requiere Docker ni WSL.

Ejecutar: ./iniciar-macos.command. En el paso 7 aún no hay endpoints ni carga de ejemplos: la tabla cursos debe estar vacía y /api/v1/cursos responderá 404. El repositorio JPA se añadió en el paso 8.

Los README, PROMPTS y archivos Postman heredados describen el laboratorio anterior; no son evidencia de la guía 2. No se reescribe su historial ni se afirma que el trabajo se realizó sin asistencia.

## Paso 7 completado

Se guardó un respaldo actualizado en ../respaldo-antes-jpa/academia_db-antes-paso7.dump y, con autorización, se eliminó únicamente cursos. La prueba de arranque de Spring Boot creó la tabla mediante Hibernate; el log conserva CREATE TABLE y la creación de UNIQUE(codigo). La entidad usa constructor protegido sin argumentos y constructor público sin id, como la guía.

Evidencias en evidencias/paso-07/: compilacion-y-creacion.txt (BUILD SUCCESS y DDL), tabla-postgresql.txt (estructura y 0 filas), verificar.sql (consulta reproducible). Se verificaron IDENTITY, clave primaria, longitudes, campos obligatorios y código único.

El historial de la guía se inicia con un commit de preparación que explica la adaptación real del proyecto anterior; no se inventaron commits separados para pasos que ya se habían realizado juntos. Se conserva la identidad Git local usada en el repositorio original. Para atribución a GitHub, revisar el correo antes de publicar.

Al cerrar el paso 7, el siguiente paso era crear CursoRepository (paso 8).

## Paso 8 — repositorio Spring Data JPA

CursoRepository extiende JpaRepository<Curso, Long> y declara los dos métodos de la guía: findByCodigoIgnoreCase y findByActivoTrueOrderByIdAsc. Spring genera sus consultas sin una implementación SQL manual. Se añadió @EnableJpaRepositories porque la clase principal está en el paquete academia_api, separado del paquete academia.repository.

Las pruebas de integración verifican guardar y leer, buscar el código sin distinguir mayúsculas, eliminar, excluir cursos inactivos y ordenar por id. Se ejecutan con PostgreSQL y transacciones revertidas al finalizar. La secuencia de ids puede avanzar aunque los registros se reviertan; es comportamiento normal de PostgreSQL.

Evidencias: evidencias/paso-08/pruebas-repositorio.txt y estado-tabla.txt. No se ha implementado el paso 9 (DTO) ni los endpoints en esta versión por etapas.

## Paso 9 — DTO de entrada y salida

CursoRequest define codigo, nombre, creditos, docente y cupos; el cliente no elige id ni activo. Se aplican las reglas de la guía: código AAA-999, nombre obligatorio de hasta 100 caracteres, docente obligatorio de hasta 80, créditos entre 1 y 6 y cupos entre 1 y 60.

CursoResponse incluye id y activo junto con los datos del curso. Su método desde convierte una entidad Curso en la respuesta que utilizará el controlador.

Se ejecutaron seis pruebas de DTO: límites válidos, campos obligatorios, formatos de código incorrectos, números fuera de rango, textos demasiado largos y conversión con tildes y estado inactivo. Resultado en evidencias/paso-09/pruebas-dto.txt. Estas pruebas no usan PostgreSQL ni modifican sus datos. La validación HTTP con @Valid se verificará al incorporar el controlador.

Siguiente paso: 10, excepciones de negocio. Aún no hay endpoints en esta versión.

## Paso 10 — excepciones de negocio

Se añadieron RecursoNoEncontradoException y ConflictoException en el paquete exception. Ambas extienden RuntimeException y conservan el mensaje recibido mediante super(mensaje), siguiendo el código de la guía.

RecursoNoEncontradoException representará un curso inexistente; ConflictoException representará un código ya utilizado. El servicio las utilizará en el paso 11. La conversión a HTTP 404 y 409 se implementará con el manejador centralizado en el paso 14; las clases por sí solas no asignan un estado HTTP.

Verificación: compilación Maven correcta. Evidencia: evidencias/paso-10/compilacion.txt. No se ejecutaron operaciones contra PostgreSQL en este paso.

Siguiente paso: 11, implementar CursoService.

## Paso 11 — servicio de cursos

CursoService concentra las reglas de negocio y recibe CursoRepository mediante el constructor. La clase usa @Transactional(readOnly = true); crear, actualizar, actualizarCupos y eliminar abren transacciones de escritura. Se recortan los espacios de nombre y docente, se rechazan códigos duplicados sin distinguir mayúsculas y se permite conservar el propio código al editar.

La implementación sigue el servicio completo mostrado en el paso 11 de la guía: incluye filtrado por docente y créditos mínimos, actualización parcial de cupos y eliminación lógica idempotente. Estos métodos todavía no tienen endpoints; el controlador y CuposRequest se incorporarán en sus pasos. Un curso desactivado se conserva y puede obtenerse por id, pero no aparece en listar.

Se usa Locale.ROOT al convertir nombres de docente a minúsculas para que el filtro no dependa de la configuración regional de la Mac. La validación Bean Validation se aplicará a las peticiones con @Valid en el controlador; el servicio mantiene las reglas de negocio.

Verificación: ocho pruebas de integración con PostgreSQL cubren creación, recorte de espacios, duplicados, actualización con código propio, conflicto con otro curso, ids inexistentes, filtros, cupos y eliminación lógica. Las pruebas fuerzan escritura y nueva lectura desde la base y revierten sus transacciones. Evidencias en evidencias/paso-11. No se ejecutaron pruebas HTTP porque todavía no existe controlador.

Siguiente paso: 12, cargar los datos de ejemplo solo si la tabla está vacía.

## Paso 12 — datos de ejemplo

DataLoader implementa CommandLineRunner y recibe CursoRepository por constructor. Si count() es mayor que cero, termina sin insertar. Si la tabla está vacía, guarda SIS-101, SIS-205 y SIS-310 con los datos de la guía. La operación se ejecuta en una transacción para que la carga de los tres registros sea completa.

Se ajustó el aislamiento de CursoServiceTests: cada prueba prepara una tabla vacía dentro de su transacción y el rollback conserva los cursos existentes. La compilación y las 17 pruebas del proyecto pasaron con los ejemplos cargados.

Las evidencias del paso 12 incluyen los datos consultados directamente en PostgreSQL y una comprobación de tres arranques (arranque inicial de la verificación y dos reinicios), comparando todas las filas y sus identificadores. Los cursos se cargaron al iniciar el contexto de pruebas; las ejecuciones posteriores verificaron que permanecieran sin cambios ni duplicados.

Los ids actuales son 15, 16 y 17: las pruebas anteriores consumieron valores de la secuencia aunque sus registros se revirtieron. PostgreSQL no garantiza ids consecutivos. Se conservó la secuencia y no se cambiaron identificadores para hacerlos coincidir con la captura de la guía.

Para repetir la verificación después de compilar: python3 evidencias/paso-12/verificar_reinicios.py. Requiere PostgreSQL iniciado, .env.local y el puerto 18080 libre. Los logs están en evidencias/paso-12/arranque-*.txt; el resumen está en reinicios.json.

Siguiente paso: 13, controlador REST. /api/v1/cursos todavía responde 404 en esta versión.

## Paso 13 — controlador REST

CursoController expone GET lista y por id, POST, PUT, PATCH /{id}/cupos y DELETE en /api/v1/cursos. Se probaron primero los dos GET y luego se añadieron las operaciones de escritura. POST responde 201 y Location; DELETE responde 204, también si el curso ya estaba inactivo. Las entradas se validan con @Valid.

Se añadió CuposRequest (rango 1–60), definido en la sección del reto y requerido por el controlador mostrado en el paso 13. Los filtros por docente y creditosMin se delegan al servicio. No hay lógica de persistencia en el controlador.

Verificación: compilación y 17 pruebas existentes correctas, más 13 peticiones HTTP contra PostgreSQL. Se verificaron GET, POST, PUT, PATCH, filtros, validación 400, protección de id/activo y DELETE lógico repetido. Evidencias en evidencias/paso-13. Las pruebas HTTP se ejecutaron con Python; no se presenta esta ejecución como una colección Postman.

SIS-400 se creó para la prueba y se conserva inactivo, con cupos=35. Los tres cursos de ejemplo permanecen activos. La tabla contiene cuatro filas y el listado devuelve tres. El script HTTP se detiene si SIS-400 ya existe; para repetirlo se debe elegir otro código y documentar la nueva ejecución.

Los casos de código duplicado y curso inexistente todavía requieren el manejador del paso 14 para responder 409 y 404. La validación actual devuelve 400, pero el detalle por campo también corresponde al siguiente paso.

Siguiente paso: 14, manejo centralizado de errores.

## Paso 14 — manejo centralizado de errores

GlobalExceptionHandler usa @RestControllerAdvice y extiende ResponseEntityExceptionHandler. Curso inexistente responde 404; código duplicado o violación de integridad responde 409. Las solicitudes inválidas responden 400 con un mapa errores por campo. JSON ilegible, parámetros de tipo incorrecto, método no permitido y tipo de contenido no admitido conservan los códigos 4xx del framework.

Las respuestas usan Problem Details (application/problem+json). Los errores inesperados responden 500 con un mensaje genérico, y la excepción completa se registra solo en el servidor. Las violaciones de integridad tampoco devuelven consultas SQL ni nombres de restricciones al cliente.

Verificación: las 19 pruebas Java del proyecto pasaron; este total no corresponde a los 19 casos Postman de la guía. Se añadieron dos pruebas directas del manejador para verificar que las respuestas 500 y de integridad no expongan detalles internos. No se añadió un endpoint que provoque errores artificiales.

La prueba HTTP del paso 14 verifica 14 respuestas de error, incluido estado, tipo de contenido Problem Details, campos de validación y ausencia de trazas. Compara todos los cursos antes y después para confirmar que no se modifican. Se prueba el duplicado de un curso existente; no se simuló una carrera entre dos altas simultáneas. Evidencias en evidencias/paso-14.

Siguiente sección de la guía: pruebas completas y revisión de seguridad. El paso 15 requiere comprobar y documentar el reto, cuya implementación ya aparece en el servicio y el controlador de los pasos 11 y 13.

## Sección 5 — pruebas completas

Se creó la colección Postman academia-guia2-pruebas.postman_collection.json y se ejecutó con Newman 6.2.1. Los 14 casos de la tabla de la guía se desglosan en 17 peticiones; con límites adicionales y cierre se ejecutaron 35 peticiones y 57 aserciones, sin fallos.

Se usaron códigos PRB-001 a PRB-007 porque SIS-400 ya existía inactivo como evidencia del paso 13. Los ids se obtienen de POST. Se comprobaron las escrituras directamente en PostgreSQL: los cuatro registros previos permanecieron iguales y las siete altas de esta ejecución quedaron inactivas. El listado sigue mostrando los tres ejemplos activos.

Evidencias: evidencias/pruebas-completas/RESULTADOS.md, resultado-newman.json, resultado-newman.xml, ejecucion-newman.txt y verificaciones-postgresql.json. La colección puede importarse en Postman; esta ejecución se realizó con Newman. El instructivo está en postman/LEEME-GUIA2.md.

Los cinco casos del reto se documentarán en el paso 15. Siguiente sección: 6, revisión de seguridad.

## Sección 6 — revisión de seguridad

Se contrastaron los ocho hallazgos del ejemplo con el código y las categorías OWASP. SEGURIDAD.md documenta evidencia y límites sin presentar una separación de capas o la ausencia de un log como prueba de una auditoría completa. Se verificaron DTO, validación de entradas grandes, errores sin trazas, filtros literales, privilegios del rol y exclusión del secreto local de Git.

Un POST sin credenciales confirmó que la autenticación sigue pendiente, tal como prevé la guía para el laboratorio siguiente. El curso SEC-001 se conserva inactivo como evidencia; las filas anteriores no cambiaron. Se identificaron también límites de consumo pendientes (paginación, longitud del filtro y frecuencia).

Se actualizó README.md, que todavía describía la versión anterior en memoria y eliminación física. Ahora coincide con PostgreSQL, JPA, eliminación lógica, endpoints y reportes actuales. No se cambiaron las reglas de la API ni se añadió el controlador inseguro al proyecto.

Siguiente: paso 15, comprobar y documentar los cinco casos del reto ya implementado.

## Paso 15 — verificación del reto

Se verificaron los cinco casos de la tabla 6 mediante una colección Postman ejecutada con Newman: PATCH válido de cupos, PATCH fuera de rango, filtro creditosMin=5, DELETE repetido y consulta del registro inactivo. Las 10 peticiones y 15 aserciones pasaron; cada escritura se contrastó con PostgreSQL.

Se crearon RET-001 (id 69) y RET-002 (id 70), que quedaron inactivos al finalizar. Los ejemplos iniciales y las demás filas existentes no cambiaron. El filtro devolvió únicamente SIS-310. Evidencias y tabla de resultados en evidencias/paso-15/RESULTADOS.md.

El código del reto ya estaba incorporado en los pasos 11 y 13, como aparece en sus ejemplos de la guía. El commit de este paso registra su verificación real, sin presentar como nuevo código una implementación que ya existía.

Los pasos funcionales y las secciones de pruebas y seguridad están completos. Para la entrega quedan organizar las dificultades reales, conclusiones, referencias e historial de commits según el formato solicitado por el curso, además de los datos personales que correspondan. No se ha publicado el repositorio ni generado un informe Word final de esta segunda guía.
