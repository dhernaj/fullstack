# Laboratorio 2 — parte C

Se implementaron los repositorios de estudiantes, matrículas y usuarios, las consultas de cursos y los servicios académicos. Se conservaron la base y los datos anteriores.

## Funciones incorporadas

- Listado paginado de cursos activos, filtros por docente y créditos mínimos y ordenación en la base. Las búsquedas derivadas tratan los comodines del filtro como texto literal.
- PaginaResponse con contenido, pagina, tamanio, totalElementos, totalPaginas y ultima. El servicio y la configuración web limitan el tamaño a 50.
- EstudianteService para crear, obtener y listar; detecta código o correo repetido y normaliza los datos antes de guardar.
- MatriculaService para matricular, consultar por curso o estudiante y retirar. Exige referencias existentes, curso activo, pareja estudiante–curso única y plazas disponibles.
- PESSIMISTIC_WRITE sobre la fila de Curso durante la matrícula. La prueba real de dos solicitudes simultáneas se reserva para el reto.
- El retiro conserva la matrícula con estado RETIRADA y libera una plaza. La misma pareja estudiante–curso no puede volver a insertarse, como establece la restricción de la guía.
- CursoService impide eliminar un curso con matrículas, según la tabla de pruebas del Laboratorio 2.
- Resumen de ocupación con proyección DTO y GROUP BY: cuenta solo matrículas activas e incluye cursos con cero matrículas.
- Consultas de detalle con JOIN FETCH y conversión a DTO dentro de la transacción. La consulta por estudiante también carga su estudiante para evitar una lectura perezosa adicional al construir el DTO.

La guía muestra completos CursoRepository, MatriculaRepository y MatriculaService, pero utiliza otros tipos sin definirlos en esa sección. Se añadieron los DTO y repositorios de apoyo y EstudianteService para completar esas dependencias, conservando los campos del modelo.

## Cambio de contrato del listado

GET /api/v1/cursos devuelve ahora un objeto paginado, no una lista simple. Se adaptó ese método del controlador existente para aceptar Pageable. Las colecciones del Laboratorio 1 se conservan como evidencia histórica y deberán actualizarse para ejecutarse contra el contrato del Laboratorio 2 y su futura autenticación JWT.

No se publicaron todavía los controladores de estudiantes o matrículas ni el resumen de ocupación. Sus funciones están comprobadas a nivel de servicio y repositorio. La configuración por rol se incorporará en la parte E.

## Pruebas

| Comprobación | H2 | PostgreSQL |
|---|---|---|
| Batería completa | 41 pruebas, 0 fallos | 41 pruebas, 0 fallos |
| Paginación, orden, filtros y límite 50 | Correcto | Correcto |
| Códigos y correos de estudiantes repetidos | Rechazados | Rechazados |
| Matrícula repetida, curso inactivo o sin plazas | Rechazados | Rechazados |
| Retiro y plaza liberada | Correcto | Correcto |
| Curso con matrículas no eliminable | Correcto | Correcto |
| Proyección de ocupación | Correcta | Correcta |
| Consultas con dos matrículas, sin fetch | 5 | 5 |
| Consultas con dos matrículas, con fetch | 2 | 2 |

El conteo incluye la consulta de existencia del curso y usa las estadísticas de Hibernate, con el contexto de persistencia limpio antes de cada estrategia. No se combina JOIN FETCH de una colección con paginación: los fetch de matrículas son relaciones individuales y las páginas de cursos se consultan por separado.

Se realizaron además cinco peticiones HTTP en un servidor H2 temporal: acceso sin credenciales 401, tamaño 100000 limitado a 50 y orden por créditos, filtro docente/créditos, segunda página y filtro literal %. Para atravesar la protección provisional se usó una cuenta Basic temporal creada solo en ese proceso; no fue una prueba JWT. La cuenta y su contraseña no quedaron persistidas ni en los reportes.

Los 14 cursos anteriores conservaron todos sus campos, incluida version. Las transacciones de prueba se revirtieron y estudiantes, matrículas y usuarios quedaron vacíos.

## Evidencias

- pruebas-h2.txt y pruebas-postgres.txt: ejecución completa y medición de consultas.
- resultados-paginacion.json y servidor-paginacion.txt: comprobación HTTP.
- probar_paginacion.py: repetición con credenciales temporales generadas en memoria.
- estado-postgresql.txt y verificacion.json: resumen de conservación y resultados.

## Siguiente parte

Parte D: ampliar el manejo de excepciones para errores de autenticación, acceso y concurrencia. Login y reglas por rol corresponden a la parte E.

Referencia de consultas y proyecciones: [Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).
