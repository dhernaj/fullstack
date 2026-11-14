# Parte E — producción y contenedores (en curso)

Se prepararon el perfil prod, la migración V1 de Flyway, el administrador inicial con contraseña BCrypt, el Dockerfile multietapa y Docker Compose con PostgreSQL 17. Las claves se guardan únicamente en archivos ignorados por Git. PostgreSQL del contenedor no publica un puerto en la Mac.

## Comprobado

- `verify.txt`: 80 pruebas aprobadas y control de cobertura satisfecho.
- `sql-postgres-temporal.txt`: SQL de la migración ejecutado en una transacción de prueba y revertido.
- `postgres-prod-local.txt`: Flyway versión 1 aplicada, un administrador con contraseña BCrypt, sin estudiantes ni matrículas de demostración.
- `http-prod-local.json`: nueve comprobaciones HTTP aprobadas. Health UP, login del administrador, rutas de documentación desactivadas, autenticación requerida y curso conservado tras reiniciar la API. El alta se comprobó antes del reinicio.
- `public-antes.txt` y `public-despues.txt`: mismas cantidades y huellas de las cuatro tablas anteriores; comparación con `cmp` sin diferencias.

Esta validación utiliza PostgreSQL nativo en el esquema exclusivo `laboratorio4_prod_prueba`, puerto HTTP 8084. El esquema public y los proyectos de los laboratorios anteriores se conservan. Incluso solicitando APP_DEMO_ENABLED=true, los cargadores quedan excluidos por el perfil prod.

El lanzador `scripts/iniciar-prod-local.command` requiere ese esquema ya preparado, el JAR construido, las credenciales locales ignoradas del backend y el archivo privado .env de la raíz. `scripts/verificar-prod-local.py` comprueba la API sin guardar contraseñas ni tokens.

## Pendiente

Ejecutar el pipeline de GitHub Actions para construir la imagen, levantar Docker Compose y comprobar salud y persistencia del volumen tras recrear los contenedores. La validación nativa no sustituye estas comprobaciones. El código se publica para poder ejecutarlo; la parte E solo se considerará verificada cuando el job de producción finalice correctamente.

Se adaptan los pasos 23 y 27 a un runner Linux de GitHub para evitar instalar la máquina virtual y las imágenes en la Mac Intel con macOS 13 y poco espacio. Se conservan Dockerfile, Compose y las comprobaciones funcionales de la guía; cambia el lugar de ejecución. No se han eliminado archivos personales ni respaldos.

El workflow .github/workflows/ci.yml genera claves aleatorias efímeras, las oculta en los logs y retira el volumen exclusivo de CI al terminar. scripts/verificar-compose.py comprueba login, ausencia de datos demo, Flyway V1, usuario sin root, puertos y persistencia tras down/up. Los resultados se publican como artefacto docker-produccion por 14 días. No se sube .env. Los otros jobs vuelven a comprobar backend/cobertura y Angular/Vitest. Se adelanta esta base del pipeline de G para validar E sin Docker local; publicación de imágenes, E2E y despliegue se completan en sus etapas.
