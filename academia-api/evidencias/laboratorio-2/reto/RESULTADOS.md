# Laboratorio 2 — resultados del reto

Los cinco casos de la tabla 4 se verificaron en H2 y PostgreSQL. La lógica de propiedad, paginación y bloqueo ya estaba incorporada al seguir los ejemplos completos de las partes C y E. Este paso registra su verificación final; no añade una segunda implementación.

## Casos de la guía

| Caso | Esperado | H2 | PostgreSQL |
|---|---|---|---|
| R1. Estudiante consulta sus matrículas | 200, dos matrículas propias | Aprobado | Aprobado |
| R2. Estudiante consulta las de otra persona | 403 | Aprobado | Aprobado |
| R3. Docente consulta al otro estudiante | 200 | Aprobado | Aprobado |
| R4. Solicitar size=100000 | tamanio=50 | Aprobado | Aprobado |
| R5. Dos POST simultáneos al último cupo | Un 201, un 409, una sola matrícula activa | Aprobado en tres rondas | Aprobado en tres rondas |

Se usaron los ids devueltos por las consultas de preparación. Cada ronda de concurrencia creó un curso nuevo con cupos=1 y utilizó dos estudiantes distintos, ninguno matriculado todavía en ese curso. Se comprobaron tanto las respuestas como las matrículas activas finales.

## Concurrencia real

Un ThreadPoolExecutor con dos trabajadores y una barrera común lanzó las dos peticiones HTTP. Se guardaron sus intervalos de ejecución y se comprobó que se solaparan. En total se enviaron doce solicitudes concurrentes, agrupadas en seis carreras.

En la primera ronda de PostgreSQL se mantuvo, desde una conexión de prueba independiente, un bloqueo de la fila durante cinco segundos. Antes de liberarlo se observaron dos conexiones de la aplicación con wait_event_type=Lock. Esto confirma que ambas solicitudes habían llegado a esperar, en lugar de inferir simultaneidad únicamente por el orden de las respuestas. Al liberarlo, una transacción ocupó la plaza y la siguiente respondió 409 tras encontrar el curso lleno.

Las dos rondas restantes de PostgreSQL y las tres de H2 se lanzaron sin ese bloqueo externo de coordinación. Todas devolvieron un 201 y un 409. El ganador varió; el resultado no depende de que gane un estudiante concreto. La espera de unos cinco segundos en la ronda coordinada fue deliberada y no es una medición del rendimiento normal de la API.

## Aislamiento de datos

H2 se inició en un proceso separado con una base nueva y se descartó al terminar. Para PostgreSQL se creó un esquema de nombre aleatorio lab2_reto_..., y se configuraron tanto currentSchema como hibernate.default_schema para dirigir todo el modelo a ese esquema.

create-drop se aplicó únicamente al servidor de prueba y a ese esquema temporal. No se cambió el perfil persistente habitual del proyecto, que mantiene update. Las cuatro tablas públicas (cursos, estudiantes, matriculas y usuarios) se compararon antes y después y conservaron exactamente sus filas. El esquema temporal se eliminó al finalizar.

Las cuentas de demostración del esquema temporal usaron contraseñas locales y hashes BCrypt. Ninguna contraseña ni JWT se incluyó en los reportes. La instancia habitual del puerto 8080 continuó funcionando en H2 y no participó en las carreras.

## Archivos

- resultados-h2.json y resultados-postgres.json: casos, intervalos, respuestas y recuentos finales.
- resumen.json: resumen de ambos perfiles y conservación del esquema público.
- servidor-h2.txt y servidor-postgres.txt: logs de los servidores temporales.
- probar_reto.py: ejecutor reproducible con las comprobaciones de aislamiento y bloqueo.

La ejecución concurrente se realizó con Python sobre HTTP real, no con una secuencia de peticiones Newman. Los reportes de Postman/Newman de la parte E se conservan como evidencia independiente de las pruebas por rol.

## Repetir

Con el ejecutable empaquetado, PostgreSQL iniciado, .env.local configurado y el puerto 18080 libre:

```sh
python3 evidencias/laboratorio-2/reto/probar_reto.py
```

El ejecutor crea y elimina su propio esquema PostgreSQL y sobrescribe los reportes de esta carpeta. Guardar una copia de las evidencias si se desea conservar varias ejecuciones. No se necesita detener la instancia de desarrollo del puerto 8080.

## Estado final

Las partes A–F y los cinco casos del reto están verificados. No se repitió la batería Maven, pues no hubo cambios funcionales en esta etapa. Las últimas 52 pruebas de cada perfil corresponden a la parte E.

Continúan documentados los pendientes de límite de intentos de login y revocación de tokens. Queda organizar el informe de entrega con dificultades reales, conclusiones, referencias y commits; no se ha publicado la rama en GitHub ni generado un informe Word final.
