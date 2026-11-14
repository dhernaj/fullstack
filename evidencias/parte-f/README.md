# Parte F — navegador real (en curso)

Se añadieron Playwright y tres pruebas del flujo principal, siguiendo los pasos 24–27:

1. Acceso a cursos sin sesión: redirección al login.
2. Clave incorrecta: aviso visible, sin ingresar al listado.
3. Administrador: login, alta con los campos completos y búsqueda por docente en el listado.

Los selectores usan etiquetas y roles de los formularios existentes. El código del curso cambia al reintentar para evitar un duplicado. La prueba de alta conserva una captura adjunta al reporte HTML. Cada caso tiene un contexto de navegador independiente.

Se requiere ADMIN_PASSWORD; no hay contraseña predeterminada. El workflow genera una clave temporal y usa la misma para el contenedor y Playwright. Las trazas de fallo de la guía se conservan solo en estos entornos efímeros; pueden incluir su tráfico de autenticación. No se usan claves de producción.

La ejecución se realiza en el job Docker, Flyway y Playwright, después de comprobar producción y persistencia. Chromium y sus bibliotecas se instalan únicamente en el runner Linux. No se descargaron navegadores ni un motor Docker en la Mac.

El artefacto playwright-report incluye HTML, JUnit, captura del curso y diagnósticos de fallo, con retención de 14 días. Se descargará al terminar la ejecución para conservar la evidencia. La enumeración local detectó los tres casos; falta acreditar su resultado real en GitHub.
