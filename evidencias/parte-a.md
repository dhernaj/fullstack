# Módulo IV — Parte A: monorepo

Fecha: 13 de noviembre de 2026. Pasos 1–3.

Se creó academia-upn con academia-api (commit de origen d0016c8) y academia-web (3484323). Las copias se extrajeron de las versiones registradas en Git; no se copiaron dependencias, compilados, cachés ni repositorios internos. Solo existe el .git de la raíz. Se eliminaron de la copia los .DS_Store registrados en el proyecto anterior.

Se inicializó un historial nuevo en main, como indica la guía. Se conservaron intactos los originales y se verificaron dos bundles con sus historiales completos en work/modulo4-respaldos, fuera del monorepo. El frontend original permanece sincronizado con GitHub en laboratorio-3; el nuevo monorepo todavía no se publicó, pues el repositorio remoto y los pipelines corresponden a la parte G.

## Configuración privada

El .gitignore raíz excluye .env, .env.local y demás variantes privadas, target, node_modules, dist, cachés y resultados temporales. Se conserva .env.example. Para las comprobaciones locales se copió la configuración privada existente del backend a academia-api/.env.local, con permisos 600; git check-ignore confirmó que no se registra.

Se adaptó el lanzador PostgreSQL heredado del frontend para apuntar a su carpeta hermana academia-api. No se cambió la configuración de PostgreSQL ni se modificaron los originales o sus ZIP.

## Comprobaciones

- JDK 17.0.12, Node.js 24.21.0 y npm del entorno existente. No se reemplazaron.
- ./mvnw package desde la copia: BUILD SUCCESS; 52 pruebas, cero fallos, cero errores, cero omitidas. Se usó el perfil H2 con demo deshabilitada durante las pruebas. Salida completa: parte-a-api-build.txt.
- npm ci desde el lockfile: instalación terminada con código 0. npm mostró avisos sobre el registro de scripts de instalación; los comandos de compilación posteriores funcionaron.
- npm run build: compilación terminada con código 0 y sin advertencias de Angular. Salida: parte-a-web-build.txt.
- Arranque real de la copia de la API con H2, demo habilitada y puerto 8082. Sin token: 401; login admin con su clave local: 200; GET cursos con Bearer: 200 y tres cursos. Los tokens y claves no se guardaron en la evidencia. Resumen: parte-a-http.json.
- ng serve del monorepo en localhost:4201: compilación y arranque correctos, respuesta HTTP 200 con el documento Angular. Captura de los logs de arranque: parte-a-web-inicio.txt.
- La automatización del navegador integrado agotó el tiempo al crear la pestaña y luego no encontró esa pestaña. No se afirma una inspección visual ni una prueba de integración desde el navegador en esta etapa. La comprobación realizada fue compilación, arranque y HTTP.
- Los logs de ejecución activos se trasladaron fuera del repositorio y se guardaron copias estáticas como evidencia, para evitar cambios posteriores en los archivos registrados.

Docker no está instalado. Se comprobará e instalará el entorno de contenedores al llegar a la parte E, antes de los cambios de sistema que resulten necesarios.

La API y Angular del Módulo IV quedan en puertos alternativos, sin detener el laboratorio anterior. La configuración de desarrollo Angular continúa apuntando al puerto habitual 8080; no se hizo login del frontend 4201 contra la copia 8082. Las pruebas de integración y E2E se organizarán en sus partes de la guía.

Sigue la parte B: revisar las pruebas conservadas, aislar su configuración y agregar cobertura JaCoCo con el mínimo requerido del 50 %.
