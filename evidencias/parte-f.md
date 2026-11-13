# Parte F — pruebas, compilación y revisión final

Fecha: 12 de noviembre de 2026. Pasos 23–25 y cierre de la persistencia pendiente de E.

## Pruebas y compilación

La prueba del componente raíz ya registraba HttpClient y Router y comprobaba la marca del navbar; se conservó y volvió a pasar dentro de la suite completa.

- npm test -- --watch=false: 93 pruebas aprobadas en 8 archivos, salida 0. Extracto real en pruebas-finales.txt.
- npm run build: salida 0 sin advertencias. Salida completa en compilacion-final.txt. Paquete inicial 500.85 kB, transferencia estimada 96.67 kB; formulario y pantallas se cargan bajo demanda.
- npm audit --json: cero vulnerabilidades reportadas, incluidas las dependencias de desarrollo. Resultado puntual en auditoria-dependencias.json.

## Revisión F1

Se revisaron los archivos de sesión, interceptor, guard, directiva, navegación, login, listado y formulario. Se corrigió un caso de respuesta 401 atrasada y se añadieron tres pruebas: conservar una sesión nueva ante un 401 anterior, tratar como texto el HTML en el nombre de un curso y hacer lo mismo con un error del formulario.

PROMPTS.md registra las decisiones y la asistencia utilizada. SEGURIDAD.md describe los controles y sus límites. No se afirma una auditoría de producción o de accesibilidad completa.

## API real y seguridad

scripts/verificar-seguridad.py ejecutó 25 comprobaciones contra la API conectada a PostgreSQL: login y lectura por los tres roles; ausencia de cookie de sesión; rechazo de POST, PUT y DELETE para docente/estudiante; falta de token; Basic y cookie; firma alterada; algoritmo none; JWT firmado pero vencido; emisor incorrecto; preflight permitido y rechazado; ausencia de cambios en el total de cursos durante las pruebas de permisos.

El detalle está en seguridad-http.json. Los tokens y claves se mantienen en memoria y no se escriben en la evidencia. La denegación de escritura se comprobó mediante solicitudes HTTP directas; no se simuló una manipulación de DevTools como si se hubiera realizado manualmente.

## Persistencia PostgreSQL y conservación del trabajo

1. Se inició PostgreSQL 17.11, instalado en la Mac Intel.
2. Se confirmó que public conservaba 14 cursos y las otras tres tablas sin filas. laboratorio3_angular no existía.
3. Se respaldó public con pg_dump en ../../work/lab3-postgres/public-antes.dump, fuera del repositorio y de la entrega. Se guardaron conteos y huellas de las cuatro tablas en postgres-public-antes.txt.
4. Se creó el esquema laboratorio3_angular en academia_db con el rol existente. La API se inició con currentSchema y hibernate.default_schema dirigidos a ese esquema, perfil postgres y los datos de demostración del backend existente. No se modificó código del backend ni se cambió el perfil de su lanzador original.
5. Se inició sesión como admin desde Angular y se creó SIS-500, Pruebas de Software, docente Docente de Laboratorio, 3 créditos y 30 cupos. Captura: postgres-curso-creado.png.
6. Un segundo intento mostró el conflicto por código duplicado. Captura: postgres-409.png.
7. Se editó únicamente ese curso nuevo a Pruebas de Software II. Captura: postgres-curso-editado.png.
8. psql confirmó una fila SIS-500 con id 4. Se detuvo y volvió a iniciar la API. SQL y Angular siguieron mostrando el curso. Los archivos antes/después del reinicio son iguales; captura: postgres-despues-reinicio.png.
9. Se repitió la comparación del esquema public. Los archivos postgres-public-antes.txt y postgres-public-despues.txt coinciden: los 14 cursos anteriores y el contenido de las otras tres tablas permanecieron intactos.

El esquema propio de este laboratorio queda guardado con cuatro cursos, dos estudiantes, dos matrículas y tres usuarios de demostración. No se sobrescribió ni eliminó la base anterior ni el ZIP inicial.

## Estado para continuar

Angular permanece en localhost:4200, la API en localhost:8080 y PostgreSQL en 127.0.0.1:5432. La pestaña quedó en el listado con sesión admin. El lanzador scripts/iniciar-api-postgres.command permite reanudar con el mismo esquema; requiere las credenciales privadas del backend, el JAR construido y PostgreSQL iniciado.

Las partes obligatorias A–F están completadas. El reto adicional es opcional y no se implementó. El despliegue y sus configuraciones de producción corresponden al siguiente módulo.
