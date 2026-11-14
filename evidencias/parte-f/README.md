# Parte F — navegador real verificado

Se añadieron Playwright y tres pruebas del flujo principal, siguiendo los pasos 24–27:

1. Acceso a cursos sin sesión: redirección al login.
2. Clave incorrecta: aviso visible, sin ingresar al listado.
3. Administrador: login, alta con los campos completos y búsqueda por docente en el listado.

Los selectores usan etiquetas y roles de los formularios existentes. El código del curso cambia al reintentar para evitar un duplicado. La prueba de alta conserva una captura adjunta al reporte HTML. Cada caso tiene un contexto de navegador independiente.

Se requiere ADMIN_PASSWORD; no hay contraseña predeterminada. El workflow genera una clave temporal y usa la misma para el contenedor y Playwright. Las trazas de fallo de la guía se conservan solo en estos entornos efímeros; pueden incluir su tráfico de autenticación. No se usan claves de producción.

La ejecución se realiza en el job Docker, Flyway y Playwright, después de comprobar producción y persistencia. Chromium y sus bibliotecas se instalan únicamente en el runner Linux. No se descargaron navegadores ni un motor Docker en la Mac.

El artefacto playwright-report incluye HTML, JUnit, captura del curso y diagnósticos de fallo, con retención de 14 días. La ejecución [43554148720](https://github.com/dhernaj/fullstack/actions/runs/43554148720), commit 00f65e3, aprobó los tres casos en Chromium (8,1 s en total) el 13 de noviembre de 2026, hora de Lima. No hubo fallos, omisiones ni reintentos. También pasaron las 80 pruebas del backend, las 97 de Angular, la compilación y las 15 comprobaciones Docker.

Se conserva el artefacto original en playwright-report.zip, el HTML en github/playwright-report/index.html y el XML en github/test-results/e2e-junit.xml. playwright-tres-aprobadas.png muestra el reporte revisado en Chrome y curso-creado.png muestra el curso creado durante la ejecución. El HTML puede consultarse mediante un servidor estático local; no requiere Docker. La parte F queda verificada.
