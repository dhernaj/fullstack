# Laboratorio 2 — parte D

Se amplió GlobalExceptionHandler según la parte D de la guía. Se extrajo el método problema para construir las respuestas de forma uniforme y conservar los mensajes anteriores.

| Situación | HTTP | Respuesta |
|---|---|---|
| Curso o estudiante inexistente | 404 | Recurso no encontrado |
| Conflicto de negocio o integridad | 409 | Conflicto de datos |
| Edición con versión obsoleta | 409 | Conflicto de concurrencia; pide volver a cargar |
| Credenciales incorrectas o usuario desconocido | 401 | Usuario o contraseña incorrectos |
| Acceso denegado | 403 | No tiene permisos para esta operación |
| Datos inválidos | 400 | Errores por campo |
| JSON ilegible o método no permitido | 400 / 405 | Estado del framework conservado |
| Error inesperado | 500 | Mensaje genérico; detalle solo en el servidor |

Las violaciones de integridad se registran con un mensaje fijo en lugar de copiar el mensaje SQL completo de la causa. El cliente tampoco recibe nombres de restricciones o consultas.

## Verificación

La batería completa de H2 pasó con 49 pruebas y cero fallos. Las ocho pruebas nuevas utilizan MockMvc standalone con el controlador de prueba y el manejador reales. Se verifican el estado HTTP, la serialización Problem Details, los mensajes genéricos y la conservación de los errores por campo.

| Prueba MVC | Resultado |
|---|---|
| Credencial incorrecta | 401 genérico, sin mensaje interno |
| Usuario desconocido | Mismo 401 y mensaje que la credencial incorrecta |
| AccessDeniedException | 403 en lugar de caer al 500 genérico |
| ObjectOptimisticLockingFailureException | 409 sin nombre de la entidad |
| Error inesperado | 500 sin traza ni mensaje interno |
| JSON mal formado | 400 |
| Validación de nombre y créditos | 400 con errores por campo |
| Método no permitido | 405 |

Estas pruebas comprueban la resolución de excepciones dentro de Spring MVC. No usan un servidor HTTP externo ni la cadena de filtros de seguridad; tampoco afirman haber probado el login JWT o los permisos por rol. Los fallos previos al controlador se configurarán con AuthenticationEntryPoint y AccessDeniedHandler en la parte E.

El controlador artificial está únicamente en src/test y se verificó que no aparezca en el ejecutable. Las pruebas no modificaron PostgreSQL; no se repitió su batería porque no se cambió el modelo ni la persistencia en esta parte.

Evidencias: pruebas-h2.txt, pruebas-mvc.xml y verificacion.json. Las contraseñas temporales de Spring Security se omitieron en el log guardado.

## Siguiente etapa

Parte E: configuración de Spring Security, contraseñas BCrypt, emisión y validación JWT, roles, CORS y controladores de las operaciones nuevas.
