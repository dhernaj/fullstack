# Academia UPN — Laboratorio 4

Alumno: Danny Manuel Herrera Nájar · 10809807

Monorepo que reúne la API del Laboratorio 2 y Angular del Laboratorio 3. Se seguirá el Módulo IV por partes A–G, con un commit por parte y evidencias de las comprobaciones.

## Parte A — organización inicial

- academia-api/: copia del backend registrado en el commit d0016c8 del Laboratorio 2.
- academia-web/: copia del frontend registrado en el commit 3484323 del Laboratorio 3.
- evidencias/: resultados del Módulo IV. Las evidencias dentro de cada proyecto corresponden a sus laboratorios anteriores.
- .gitignore: excluye configuraciones privadas, compilados, cachés y dependencias.

Se creó un nuevo historial Git en main, como pide la guía. Los repositorios originales permanecen intactos y sus historiales completos se respaldaron como bundles en la carpeta de trabajo work/modulo4-respaldos, fuera de este repositorio. No se copiaron los .git internos.

La Mac Intel dispone de JDK 17 y Node.js 24.21.0, compatible con el Angular 22 del proyecto existente. Se conserva esa versión. Docker no estaba instalado durante la parte A; se comprobará el entorno de contenedores al llegar a la parte E.

La configuración privada .env.local del backend está excluida de Git. Los ejemplos de configuración no contienen contraseñas. No se modificaron PostgreSQL ni los ZIP de las entregas anteriores para organizar este monorepo.

## Comprobaciones locales

Backend, desde academia-api, con las variables privadas del Laboratorio 2 cargadas:

```sh
./mvnw package
./mvnw spring-boot:run
```

Frontend, desde academia-web:

```sh
npm ci
npm test -- --watch=false
npm run build
npm start
```

Para las verificaciones de traslado se usan H2 y puertos alternativos si las aplicaciones del Laboratorio 3 siguen abiertas. La configuración normal de desarrollo continúa apuntando a http://localhost:8080/api/v1.

El lanzador academia-web/scripts/iniciar-api-postgres.command se adaptó a la carpeta hermana academia-api. Conserva el esquema laboratorio3_angular del laboratorio anterior y requiere PostgreSQL iniciado, las credenciales privadas y el JAR construido; no prepara el futuro entorno Docker del Módulo IV.

La parte B añade pruebas unitarias, de persistencia e integración y cobertura JaCoCo, conservando las pruebas anteriores.

## Parte B — pruebas del backend y cobertura

Las pruebas tienen configuración propia en academia-api/src/test/resources/application.properties. H2 queda únicamente en las dependencias de test; el perfil normal de esta copia cambia a PostgreSQL. Las claves locales no son necesarias para ejecutar la suite. Los datos y usuarios de las pruebas se crean dentro de H2 y las transacciones se revierten.

```sh
cd academia-api
./mvnw verify
```

JaCoCo 0.8.13 genera el reporte HTML en target/site/jacoco/index.html y comprueba un mínimo global del 50 % de líneas, sin exclusiones de clases. El mínimo está parametrizado en jacoco.minimum.line.coverage para que se pueda comprobar también el rechazo de un umbral superior, sin dejar el pom.xml modificado.

Se añadieron CursoServiceTest y MatriculaServiceTest con Mockito, CursoRepositoryTest con @DataJpaTest y CursoApiIntegrationTest con MockMvc y login JWT real. El contexto de aplicación se indica explícitamente en las pruebas que están fuera del paquete del arranque. Las cuentas y la clave JWT de test son ficticias y diferentes de las credenciales locales. La clave se proporciona en Base64, como exige SecurityConfig; los tokens de prueba duran cinco minutos.

La prueba de mutación puede repetirse desde la raíz con python3 scripts/comprobar-mutacion.py. Modifica temporalmente la comparación de cupos, ejecuta su prueba y restaura la fuente en un bloque finally. Después se debe ejecutar ./mvnw clean verify desde academia-api para regenerar clases y cobertura con la regla restaurada.

Las evidencias de esta parte están en evidencias/parte-b/. La siguiente parte de la guía corresponde a las pruebas del frontend.

Para comprobar el límite de cobertura sin repetir las pruebas ni modificar el pom.xml, después de verify se puede ejecutar python3 scripts/comprobar-umbral.py desde la raíz. El script exige un rechazo al 95 % y una aprobación al 50 %. prepare-agent inicia un archivo de cobertura nuevo en cada ejecución para no mezclar datos de intentos anteriores.

Se corrigió también el script de seguridad heredado en academia-web/scripts/verificar-seguridad.py: usa la clave decodificada de Base64, comprueba primero un JWT vigente y después los JWT vencidos o con emisor incorrecto. Acepta LAB_API_URL y LAB_EVIDENCIA_SEGURIDAD para probar una instancia local separada sin sobrescribir la evidencia histórica del Laboratorio 3. El resultado de esta comprobación está en evidencias/parte-b/jwt-http.json.

Resultado de la parte B: 70 pruebas aprobadas, 86,87 % de cobertura global de líneas y 100 % en servicios. El rechazo del umbral del 95 % y la aprobación del 50 % quedaron acreditados. El reporte portable está en evidencias/parte-b/jacoco-reporte.zip; consultar evidencias/parte-b/README.md para el detalle.

## Parte C — pruebas del frontend

Se conservaron las pruebas del Laboratorio 3 y se completaron los pasos 11–13 de la guía. CursoService se prueba con HttpTestingController: parámetros de paginación y orden, docente recortado, filtros vacíos, respuesta paginada y operaciones GET/POST/PUT/DELETE. El contrato Pagina conserva ultima, presente en nuestra API.

El archivo auth.interceptor.spec.ts comprueba el interceptor de forma aislada, con AuthService sustituido por un doble de prueba: Bearer a nuestra API, preservación de cabeceras, ausencia de token y logout ante un 401 ajeno, y cierre de la sesión ante su propio 401. Las pruebas anteriores de sesión e interceptor con el servicio real también se conservaron.

```sh
cd academia-web
npm test -- --watch=false
```

No se necesitan API, base de datos, contraseñas ni tokens reales: los HTTP se atienden con provideHttpClientTesting. Las dependencias ya se habían instalado desde package-lock.json en la parte A, por lo que no se repitió npm install ni se modificó el lockfile. La salida de esta parte está en evidencias/parte-c/vitest.txt.

Resultado de la parte C: 97 pruebas aprobadas en nueve archivos, sin cambiar código de funcionamiento ni dependencias. La salida completa y el detalle de los casos están en evidencias/parte-c/. Sigue la parte D: OpenAPI y Actuator.

## Parte D — OpenAPI, Swagger UI y estado de la API

Se añadió springdoc-openapi 3.2.0, compatible con nuestro Spring Boot 4.1.2, y Actuator. La versión 3.0.3 del ejemplo corresponde a la línea Boot 4.0; se adaptó la dependencia según la matriz oficial de springdoc.

- /swagger-ui.html: interfaz de documentación con secciones Cursos, Estudiantes, Matrículas y Autenticación.
- /v3/api-docs: contrato JSON con Bearer JWT, paginación y filtros. El login se declara público.
- /actuator/health: acceso sin token y estado UP, sin detalles de base de datos.

Se documentaron las operaciones de cursos, incluida la eliminación lógica, el PATCH de cupos, las respuestas y los errores application/problem+json. Las reglas académicas previas y anyRequest().denyAll() se conservaron; solo se abrieron las rutas de documentación y health. Swagger no guarda la autorización al recargar.

Para revisar la interfaz sin usar PostgreSQL ni claves privadas:

```sh
./scripts/iniciar-documentacion.command
```

Abrir http://127.0.0.1:8083/swagger-ui.html. Este lanzador incluye el classpath de test para H2, usa un perfil doc-preview y credenciales ficticias de demostración, y escucha únicamente en loopback. El usuario admin usa la clave de demostración declarada en ese script. No es una configuración de producción. Los datos de esta vista se descartan al reiniciar.

La documentación se comprobó en Chrome: petición sin token 401, login, Authorize y POST con 201/Location. Se verificaron 75 pruebas del backend y 12 comprobaciones HTTP de documentación. Evidencias y contrato exportado en evidencias/parte-d/. También se completó en Chrome la revisión visual del login Angular del monorepo que había quedado pendiente en A.

Sigue la parte E: Flyway, configuración de producción y Docker. La desactivación de Swagger en producción se configurará en esa etapa, como indica la guía.

## Parte E — producción y Docker

Se prepararon Flyway V1, el perfil prod, un administrador inicial con contraseña BCrypt, el Dockerfile multietapa y Docker Compose con PostgreSQL 17. Swagger y los cargadores de demostración quedan desactivados en producción. El perfil de desarrollo conserva Flyway desactivado para no migrar automáticamente la base anterior.

Las 80 pruebas del backend pasan. La ejecución adicional con PostgreSQL nativo en el esquema aislado laboratorio4_prod_prueba confirmó la migración, el login y la escritura de un curso, sin cambios en las tablas public anteriores. Esta ejecución no es una prueba de Docker.

La construcción y ejecución de los contenedores se traslada a GitHub Actions para evitar instalar Docker en la Mac. El workflow ci.yml genera claves temporales y comprueba Docker, Flyway, puertos, usuario sin root y persistencia tras recrear contenedores. Publica las evidencias en el artefacto docker-produccion. Se adelanta esta base de G para poder verificar E; los despliegues se añadirán después. La ejecución [43553764567](https://github.com/dhernaj/fullstack/actions/runs/43553764567) aprobó los tres jobs y las 15 comprobaciones de Docker. Evidencias descargadas en evidencias/parte-e/github/ y docker-produccion.zip. La parte E queda verificada mediante esta adaptación, sin instalar Docker localmente.
