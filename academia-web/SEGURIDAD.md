# Revisión de seguridad del Laboratorio 3

Revisión local del 12 de noviembre de 2026, con la API del Laboratorio 2 y PostgreSQL 17.11. Es una verificación del laboratorio, no una certificación para producción.

## Controles verificados

- JWT enviado únicamente al mismo origen y a la raíz exacta de la API o sus subrutas. Se rechazan prefijos parecidos y otros orígenes. No se adjunta un token anterior al login.
- Sesión en sessionStorage, sin contraseña guardada. Se descartan datos dañados, incompletos o vencidos; el temporizador limpia la sesión al vencer.
- Un 401 de la API protegida cierra la sesión correspondiente. Un 401 atrasado de un token anterior no elimina una sesión nueva. Un 403 no borra una sesión vigente.
- Un guard protege el listado; las rutas de creación y edición exigen ADMIN. La directiva adapta los botones al rol. Estos controles del navegador se pueden modificar y no reemplazan la autorización real de la API.
- La API rechazó escrituras de DOCENTE y ESTUDIANTE, tokens alterados, algoritmo none, JWT firmado pero vencido y emisor incorrecto; también rechazó Basic y token en cookie. El preflight permite localhost:4200 y rechaza un origen ajeno.
- Los datos y errores se muestran por interpolación, sin innerHTML ni bypassSecurityTrust. Las pruebas con etiquetas HTML no crearon elementos en el DOM.
- Los formularios validan campos y bloquean envíos repetidos. La búsqueda cancela respuestas anteriores y las suscripciones se liberan al abandonar las pantallas.
- Etiquetas de campos, mensajes asociados, regiones de estado y alerta, encabezados y paginación revisados. No se realizó una auditoría completa de accesibilidad.

## Evidencia

- auth.spec.ts, curso-list.spec.ts y curso-form.spec.ts: casos de sesión y regresión frente a HTML.
- evidencias/seguridad-http.json: 25 comprobaciones reales, sin tokens ni contraseñas.
- evidencias/auditoria-dependencias.json: npm audit reportó cero vulnerabilidades en la consulta realizada.
- evidencias/postgres-public-antes.txt y postgres-public-despues.txt: filas y huellas de las cuatro tablas anteriores coinciden.
- evidencias/postgres-curso-antes-reinicio.txt y postgres-curso-despues-reinicio.txt: SIS-500 permanece guardado tras reiniciar la API.

## Límites del entorno

sessionStorage es accesible desde JavaScript: una vulnerabilidad XSS podría leer el token. La separación por pestaña reduce persistencia, pero el comportamiento de restauración de pestañas del navegador puede conservar la sesión. La guía no proporciona una garantía de borrado absoluto al cerrar el navegador.

Salir elimina la sesión del cliente; no revoca un JWT ya emitido. La API usa expiración de 60 minutos. La revocación y el límite de intentos de login del backend quedan fuera de esta implementación del frontend.

El laboratorio usa HTTP en loopback. HTTPS, cabeceras CSP/Trusted Types y la URL definitiva de producción deben configurarse al desplegar en el Módulo IV. No se incluyeron contraseñas de demostración ni el secreto JWT en el código Angular o las evidencias.

Referencias: [Seguridad de Angular](https://angular.dev/best-practices/security) y [guards y autorización del servidor](https://angular.dev/guide/routing/route-guards).
