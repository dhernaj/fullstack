# Módulo IV — Parte B: pruebas y cobertura del backend

Fecha: 13 de noviembre de 2026. Pasos 4–10 de la guía.

## Resultado final

- 70 pruebas: cero fallos, cero errores y cero omitidas. Se conservaron las 52 anteriores y se añadieron 18.
- JaCoCo 0.8.13: 344 líneas cubiertas y 52 sin cubrir; cobertura global 86,87 %.
- Paquete de servicios: 90 de 90 líneas cubiertas, 100 %.
- Mínimo global configurado: 50 %, aprobado. No se excluyeron clases para alcanzar el mínimo.
- Comprobación temporal del 95 %: rechazada por cobertura insuficiente. Comprobación posterior del 50 %: aprobada.
- Mutación del límite de cupos: detectada por la prueba; el servicio quedó restaurado antes de la ejecución final.

## Configuración de pruebas

Se agregaron los starters de prueba JPA y Security de Spring Boot 4.1.2; se conservó el de Web MVC. H2 tiene alcance test. La ejecución normal de esta copia usa PostgreSQL y se retiró el perfil H2 de producción; el lanzador macOS se ajustó al perfil PostgreSQL. Las copias originales de los laboratorios no cambiaron.

src/test/resources/application.properties configura H2 en memoria, create-drop, sin Flyway ni demo y con una clave JWT pública de test en Base64. Los tokens de prueba duran cinco minutos; el tiempo de producción no cambió. La prueba anterior de JWT se adaptó a esa duración y comprueba también el intervalo del token emitido.

La ejecución final fue ./mvnw -B clean verify, quitando del entorno JWT_SECRET, DB_URL, DB_USERNAME, DB_PASSWORD, las claves LAB_*, APP_DEMO_ENABLED y SPRING_PROFILES_ACTIVE. No se cargó .env.local. Una prueba verifica DatabaseProductName H2 y una URL jdbc:h2:mem:. PostgreSQL no se detuvo, ni se utilizó para estas pruebas: se acreditó la independencia con la conexión H2 y sin credenciales locales, preservando los servicios anteriores.

## Pruebas añadidas

| Clase | Casos | Qué comprueba |
| --- | ---: | --- |
| CursoServiceTest | 4 | Creación normalizada, duplicados, recurso inexistente y actualización con el propio código |
| MatriculaServiceTest | 4 | Duplicado, curso lleno, curso inactivo y último cupo disponible |
| CursoRepositoryTest | 2 | Consulta por docente y conteo exclusivo de matrículas activas con JPA y H2 |
| CursoApiIntegrationTest | 8 | Conexión H2, 401, login incorrecto, paginación, 403, alta 201/Location, validación 400 y propiedad de matrículas |

Las pruebas unitarias no levantan Spring. La prueba @DataJpaTest usa el contexto indicado explícitamente porque el arranque está en el paquete hermano pe.edu.upn.academia_api. Los tests de integración usan MockMvc, filtros y JWT reales dentro del contexto de Spring, sin abrir un servidor de red. Crean cuentas ficticias dentro de transacciones que se revierten; no dependen del bootstrap de demostración ni de ids fijos.

Se adaptó el ejemplo al código existente: CursoService recibe también MatriculaRepository y el máximo de página, actualizar guarda la entidad y CursoResumen no contiene disponibles. Por ello se construye el servicio con máximo 50, se configura el save del mock y se comprueba la disponibilidad como cupos menos matriculados. No se cambió el contrato de la API para copiar literalmente el ejemplo.

## Mutación manual del paso 7

scripts/comprobar-mutacion.py cambió temporalmente ocupados >= cupos por ocupados > cupos. La prueba rechazaCursoSinCupos falló con código de salida 1. Un bloque finally restauró exactamente los bytes originales; mutacion-resultado.json registra la huella y la comprobación. La fuente de MatriculaService coincide con el commit anterior y la ejecución final completa volvió a pasar.

## Umbral del paso 10

scripts/comprobar-umbral.py ejecutó jacoco:check@check con jacoco.minimum.line.coverage=0.95, utilizando el reporte de la suite final. Falló por cobertura insuficiente. Repitió el mismo check con 0.50 y pasó. Esto comprueba el control sin repetir los tests ni dejar el pom.xml alterado.

## Incidencias corregidas durante esta parte

La primera configuración de test usó inicialmente una clave de texto plano; SecurityConfig requiere Base64. Se corrigió antes del resultado final. Una ejecución posterior pasó las 70 pruebas, pero el reporte no pudo leer el archivo acumulado de cobertura. Se regeneró con clean verify y prepare-agent append=false. Los logs intermedios están fuera del repositorio en work/modulo4-respaldos; verify.txt contiene la ejecución final correcta.

Se corrigió además el script de seguridad heredado del frontend: la firma HMAC debe usar los bytes decodificados de JWT_SECRET. Se añadió un JWT vigente de control, esperado 200, para evitar confundir una firma inválida con una expiración o un emisor rechazados. El token vencido se sitúa cinco minutos en el pasado. Se ejecutaron 26 comprobaciones HTTP contra la instancia H2 de la parte A en 127.0.0.1:8082, con claves locales solo en memoria; el resultado sin secretos está en jwt-http.json. No se sobrescribió la evidencia histórica del Laboratorio 3 ni su repositorio publicado.

## Archivos de evidencia

- unitarias.txt: las ocho pruebas unitarias nuevas en verde antes de la mutación.
- mutacion-cupos.txt y mutacion-resultado.json: fallo esperado y restauración.
- verify.txt: suite final de 70 pruebas, reporte y mínimo aprobado.
- umbral-95.txt, umbral-50.txt y cobertura-resumen.json: comprobaciones de cobertura.
- jacoco.xml, jacoco.csv y jacoco-reporte.zip: reporte conservado; extraer el ZIP y abrir jacoco/index.html.
- empaquetado.json: el JAR incluye PostgreSQL, no incluye H2 ni configuración de test, y exige JWT_SECRET para producción.
- jwt-http.json: comprobación adicional de JWT y autorización real en la instancia local de la parte A.

El HTML de trabajo también está en academia-api/target/site/jacoco/index.html. Se verificaron los contadores XML, la integridad del ZIP y el contenido del JAR. No se realizó una inspección visual del reporte con el navegador automatizado.

Referencia técnica: https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html y https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html.

Sigue la parte C: comprobar las pruebas del frontend con Vitest y conservar sus evidencias.
