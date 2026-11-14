# Laboratorio 2 — parte A

Se prepararon dependencias y configuración sobre la base del Laboratorio 1, en la rama laboratorio-2. La rama main conserva el cierre anterior.

## Cambios

- H2 para desarrollo, perfil predeterminado h2 con create-drop y base en memoria.
- Perfil postgres con la base existente y ddl-auto=update; SQL en nivel info.
- Spring Security y el resource server OAuth2 para la futura configuración JWT. Se usa el nombre modular spring-boot-starter-security-oauth2-resource-server de Spring Boot 4.
- open-in-view=false, formato SQL legible, máximo de página 50, origen CORS de desarrollo y expiración JWT de 60 minutos. El límite de página se aplicará al incorporar Pageable; CORS y JWT se conectarán a la configuración de seguridad en la parte E.
- JWT_SECRET aleatorio de 32 bytes en Base64, guardado solo en .env.local. La propiedad no incorpora la clave de ejemplo de la guía.
- El script de inicio permite elegir H2 o PostgreSQL mediante SPRING_PROFILES_ACTIVE.

## Verificación

| Comprobación | Resultado |
|---|---|
| Compilación y pruebas con H2 | 19 pruebas, 0 fallos; ejecutable generado |
| Pruebas con PostgreSQL | 19 pruebas, 0 fallos |
| Cursos antes/después en PostgreSQL | Todas las filas e identificadores conservados |
| Dependencias del ejecutable | H2, Spring Security, OAuth2 Resource Server y Nimbus presentes |
| Servidor con perfil H2, sin credenciales | HTTP 401 por la configuración predeterminada |
| Secretos | .env.local no versionado; contraseñas temporales omitidas en los logs guardados |

El 401 es provisional y no demuestra que la emisión o validación JWT esté implementada. Login, roles, entidades nuevas y seguridad por endpoint siguen pendientes. Se conservaron los reportes del Laboratorio 1 como evidencia histórica.

## Repetir las pruebas

```sh
source ./.env.local
./mvnw clean package
./mvnw -Dspring.profiles.active=postgres test
```

Para la segunda ejecución, PostgreSQL debe estar iniciado. Las pruebas actuales revierten sus cambios en las tablas; las secuencias pueden avanzar.

## Siguiente parte

Parte B: entidades Estudiante, Matricula, Usuario, enumeraciones y relaciones con Curso, siguiendo el modelo del documento Modulo2_Informe_Laboratorio_Alumno.docx.

Referencia del nombre modular de la dependencia: [Spring Boot 4 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).
