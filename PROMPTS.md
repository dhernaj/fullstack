# Revisión con apoyo de IA — Laboratorio 3

Fecha: 12 de noviembre de 2026.

El asistente participó en la implementación, las pruebas y esta revisión. Este archivo registra las decisiones de revisión; no afirma que el código se haya escrito sin asistencia ni que se haya realizado una consulta externa independiente.

## Pauta F1 de la guía

Se usó como pauta revisar Angular 22, signals, componentes standalone, suscripciones, peticiones duplicadas, XSS, almacenamiento y envío del JWT, guards y accesibilidad de formularios/tablas.

Archivos revisados: auth.service.ts, auth.interceptor.ts, auth.guard.ts, has-role.directive.ts, navbar.ts, login.ts, curso-list.ts, curso-form.ts y sus plantillas y pruebas.

## Hallazgos y decisiones

| Revisión | Decisión y evidencia |
| --- | --- |
| Un 401 de una petición anterior podía borrar una sesión nueva | Aplicada en F: cerrar sesión solo si el token de la petición coincide con el actual. Prueba de regresión en auth.spec.ts. |
| Datos o mensajes del servidor podrían contener etiquetas HTML | Se conservaron las interpolaciones, sin innerHTML ni bypassSecurityTrust. Se añadieron pruebas con etiquetas en el nombre de un curso y en un error 409. No se insertan elementos img. |
| Suscripciones y consultas atrasadas | Verificado: takeUntilDestroyed, switchMap en búsqueda y cancelación de la lectura de edición al cambiar de id. Se conservó la implementación ya probada. |
| La guía y la API usaban nombres distintos en el login | Adaptación previa conservada: tokenType, expiresIn y username coinciden con el backend. |
| Una sesión dañada podía impedir el ingreso | Se conservó el descarte de JSON inválido, contrato incompleto y sesiones vencidas. |
| Angular puede enlazar volver como undefined | Corrección realizada en E y verificada en el navegador: usar /cursos por defecto. |
| La autorización visual se puede modificar en el navegador | Se conservan guards y directiva como ayudas de interfaz. La API rechazó POST, PUT y DELETE de docente/estudiante con 403. |
| Roles técnicos de Spring aparecían junto al rol académico | Se conservó el filtro de roles ADMIN, DOCENTE y ESTUDIANTE para la interfaz. No se modifica el JWT. |
| Formularios y tablas | Se revisaron etiquetas, tipos de input, aria-describedby, aria-invalid, estados, encabezados de tabla, paginación y botones deshabilitados. No se afirma una auditoría WCAG completa. |
| Cambiar el token a una cookie HttpOnly | No aplicado: la guía trabaja con Bearer y sessionStorage. Se documenta la exposición frente a XSS; no se presenta sessionStorage como una defensa suficiente. |
| CSP, HTTPS, revocación y límite de intentos | No se introducen cambios de despliegue ni del backend en este módulo. Se registran los límites en SEGURIDAD.md. |

## Verificación

93 pruebas Angular aprobadas; 25 comprobaciones HTTP reales con PostgreSQL; auditoría npm con cero vulnerabilidades reportadas; SQL antes y después del reinicio; comparación de las cuatro tablas del esquema public sin cambios. Las salidas y capturas están en evidencias/.
