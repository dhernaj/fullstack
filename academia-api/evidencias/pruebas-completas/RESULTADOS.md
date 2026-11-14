# Resultados de pruebas completas

Colección Postman v2.1 ejecutada con Newman 6.2.1. Resultado: 35 peticiones, 57 aserciones, 0 fallos. Esta ejecución se realizó con Newman, no en la interfaz gráfica de Postman.

Los códigos PRB-001 a PRB-007 estaban libres. Se utilizaron los ids devueltos por POST, sin suponer que el siguiente id sería 4. Los cuatro registros anteriores se conservaron y los siete creados quedaron inactivos al finalizar.

| Caso de la guía | HTTP esperado | HTTP obtenido | Resultado |
|---|---|---|---|
| 01 - Crear curso válido | 201 | 201 | Aprobado |
| 02 - Código duplicado | 409 | 409 | Aprobado |
| 03 - Validación por campo | 400 | 400 | Aprobado |
| 04 - Formato de código | 400 | 400 | Aprobado |
| 05 - JSON inválido | 400 | 400 | Aprobado |
| 06 - Actualizar curso | 200 | 200 | Aprobado |
| 07 - Actualizar inexistente | 404 | 404 | Aprobado |
| 08 - Identificador inválido | 400 | 400 | Aprobado |
| 09 - Eliminación lógica | 204 | 204 | Aprobado |
| 10 - DELETE idempotente | 204 | 204 | Aprobado |
| 11 - Créditos válidos en los límites | 201, 201 | 201, 201 | Aprobado |
| 12 - Créditos fuera de rango | 400, 400 | 400, 400 | Aprobado |
| 13 - Límite de cupos | 201, 400 | 201, 400 | Aprobado |
| 14 - Longitud de nombre | 400 | 400 | Aprobado |

Se añadieron comprobaciones de cupos 0 y 1, nombre de 100 caracteres, docente de 80/81 caracteres, campos vacíos, espacios y formatos de código. La fase final comprueba el registro inactivo y desactiva las altas de límites válidos.

Cada petición tiene un registro de PostgreSQL en verificaciones-postgresql.json. Después de POST y PUT se comparan los campos de respuesta con la fila almacenada; después de DELETE se confirma que el registro existe con activo=false.

Los cinco casos del reto (PATCH, filtros y desactivación) se documentarán por separado al completar el paso 15. Las 35 peticiones de esta colección no se presentan como los 19 casos finales de la guía.
