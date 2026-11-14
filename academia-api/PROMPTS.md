# Registro de uso de IA

Proyecto academia-api del laboratorio Módulo I.


## 1 Entidad y estructura de paquetes

Prompt reconstruido: «Ayúdame a continuar desde la Parte A de la guía y crear los paquetes y la clase Curso en Java 17, sin Lombok ni JPA».

Respuesta resumida: creación de los paquetes y un POJO con campos del catálogo, constructores, getters, setters y activo inicialmente true.

Revisión: se respetaron los campos y tipos de la guía. El usuario creó los archivos en VS Code y compartió una compilación con BUILD SUCCESS. No se añadieron dependencias para generar accesores.

## 2 Contrato de entrada y salida

Prompt reconstruido: «Guíame para crear CursoRequest y CursoResponse con las validaciones del laboratorio».

Respuesta resumida: records de entrada y salida; restricciones Jakarta Validation y conversión desde Curso.

Revisión: el DTO de entrada excluye id y activo. Se comprobó posteriormente que nombre vacío y creditos 9 producen 400 con errores por campo. La prueba de asignación de campos internos recibió id 8 y activo true pese a enviar id 999 y activo false.

## 3 Repositorio y detección de componentes

Prompt reconstruido: «Continúa con el repositorio en memoria y revisa las capturas de mi clase principal».

Respuesta resumida: interfaz CursoRepository, implementación con ConcurrentHashMap y AtomicLong, y carga de tres cursos.

Corrección aplicada: la clase principal estaba en pe.edu.upn.academia_api y las capas en pe.edu.upn.academia. Se agregó scanBasePackages a @SpringBootApplication. Se observó el mensaje Cursos iniciales cargados: 3.

Análisis crítico: las estructuras concurrentes no hacen atómica la comprobación de código duplicado y su guardado. No se realizaron pruebas concurrentes.

## 4 Servicio controlador y errores

Prompt reconstruido: «Implementa las reglas del servicio, los cinco endpoints REST y el manejo de errores conforme a la guía».

Respuesta resumida: servicio, controlador, excepciones de negocio y manejador global basado en ResponseEntityExceptionHandler.

Revisión: se comprobaron 201 con Location, 200 en actualización, 204 en eliminación, 409 por duplicado y 400/404 para las entradas inválidas o inexistentes. El servidor se había detenido con Ctrl+C; el rechazo de conexión se resolvió manteniendo el proceso activo, sin cambiar el código de los endpoints.

## 5 Automatización en Postman

Prompt reconstruido: «Guíame paso a paso para usar Postman y comprobar los casos del laboratorio con scripts».

Respuesta resumida: colección ordenada de 00 a 10, scripts pm.test y variable cursoId guardada al crear un curso.

Correcciones y decisiones: se adaptaron las instrucciones a la interfaz observada, que mostraba After response y Test Results en un menú inferior. Se mantuvo baseUrl como origen del servidor y /api/v1 en las rutas. Se conservaron los errores intencionales en los casos negativos frente a las sugerencias automáticas de reparación.

Verificación: el resumen de Postman mostró 24 comprobaciones aprobadas, cero fallidas y cero errores.

## 6 Reporte con Newman

Prompt reconstruido: «Postman pide membresía para exportar resultados; ayúdame a generar el JSON por otra vía».

Respuesta resumida: ejecución de la colección exportada con Newman, los reporteros cli,json y baseUrl configurada mediante --env-var.

Corrección: la indicación inicial de exportar resultados desde la interfaz resultó bloqueada por el plan. Se generó un reporte nuevo mediante Newman, sin presentarlo como exportación de la ejecución anterior.

Verificación: se adjuntó el JSON y se leyó su contenido: 11 peticiones, 24 aserciones, cero fallos y ninguna pendiente.

## 7 Revisión de seguridad y compilación final

Prompt reconstruido: «Revisa los resultados de seguridad y mi pom.xml antes de documentar la entrega».

Respuesta resumida: revisión del POM compartido, comparación con el controlador inseguro de la guía y comprobaciones manuales adicionales.

Verificación: el nombre de 5000 caracteres produjo 400 con máximo 100; se eliminó el curso de prueba con 204. El POM no fija versiones en los starters. La captura de clean verify mostró BUILD SUCCESS y la generación del JAR.

Límites: no se inspeccionó todo el repositorio local, no se comprobó la eliminación de un controlador inseguro ni se ejecutó un escáner de dependencias. La captura final no muestra el detalle de pruebas Java. Autenticación y autorización siguen pendientes.

## Aprendizajes

Las respuestas de IA se contrastaron con la guía, compilaciones, respuestas HTTP, capturas de Postman y un reporte JSON. Un estado HTTP 400, 404 o 409 puede ser un resultado correcto cuando el caso de prueba lo espera. El éxito de estas pruebas no demuestra ausencia de todas las vulnerabilidades.

Se aplicó la refactorización del método listar propuesta por la IA. Se conservó el comportamiento de búsqueda y se verificó con 24 comprobaciones de Newman sin fallos y una consulta manual con mayúsculas y minúsculas combinadas.
