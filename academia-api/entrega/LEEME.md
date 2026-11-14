# Entrega del Laboratorio 2

Estudiante: Danny Manuel Herrera Nájar. Código: 10809807.
Correo: danny.herrera@upn.edu.pe.

El informe Word resume las partes A a F, los doce casos de seguridad, el reto, las dificultades reales y los commits. Completar docente y sección en la portada antes de entregar.

El proyecto está en la rama laboratorio-2; main conserva el laboratorio anterior. El remoto configurado es https://github.com/dhernaj/fullstack.git. Los commits siguen locales: esta preparación no publica la rama.

## Archivos principales

- Informe_Laboratorio2_Danny_Herrera.docx: informe final editable.
- README.md: ejecución y arquitectura.
- SEGURIDAD.md: revisión actual y pendientes.
- postman/academia-lab2-seguridad.postman_collection.json: colección importable.
- evidencias/laboratorio-2: resultados por etapa y ejecutores.
- db/laboratorio2: actualización de la versión de cursos existentes.

Los archivos de la primera guía se conservan como antecedentes y no se presentan como evidencia del segundo laboratorio. El informe utiliza los resultados de laboratorio-2.

## Configurar las credenciales en otra copia

Requisitos: Java 17 y acceso a las dependencias Maven. H2 no necesita instalar PostgreSQL.

1. Copiar .env.example a .env.local y dar permisos 600.
2. Generar JWT_SECRET una sola vez con openssl rand -base64 32 y pegar el resultado en .env.local.
3. Elegir tres contraseñas locales diferentes, de al menos 12 caracteres, para las variables LAB_*_PASSWORD.
4. Ejecutar ./iniciar-macos.command para H2. Se crean tres cuentas demo; no se comparten sus contraseñas en esta entrega.
5. Para PostgreSQL, configurar antes la base academia_db, su usuario academia_app y DB_PASSWORD; después usar SPRING_PROFILES_ACTIVE=postgres ./iniciar-macos.command. En una base previa del Laboratorio 1, aplicar primero db/laboratorio2/01_inicializar_version.sql.

No se incluyen .env.local, respaldos de bases, contraseñas, tokens, archivos target ni cachés. Los reportes de Newman están depurados y conservan los estados y aserciones. Las pruebas del reto usan HTTP concurrente con Python, no una secuencia Newman.

## Historial Git

El ZIP de entrega incluye academia-historial.bundle. Para recuperar los commits:

```sh
git clone academia-historial.bundle academia-con-historial
cd academia-con-historial
git switch laboratorio-2
```

La carpeta academia-api del ZIP es una copia de los archivos versionados y no contiene .git; el bundle conserva el historial. Los datos de prueba de los servidores temporales no forman parte del paquete.
