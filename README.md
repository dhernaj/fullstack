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

Sigue la parte B: pruebas del backend, aislamiento de su configuración y cobertura JaCoCo. Las pruebas anteriores se conservarán y se completarán conforme a la guía.
