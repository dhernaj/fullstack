# Paso 15 — resultados del reto

Los cinco casos de la tabla 6 de la guía se ejecutaron con Newman 6.2.1 sobre una colección Postman v2.1. Se usaron dos cursos temporales para conservar los ejemplos iniciales. Resultado: 10 peticiones, 15 aserciones y cero fallos, además de las verificaciones directas de PostgreSQL.

| Caso | Petición | Resultado HTTP | Comprobación |
|---|---|---|---|
| R1 | PATCH /cursos/69/cupos con cupos=35 | 200 | RET-001 quedó con 35 cupos; los demás campos no cambiaron. |
| R2 | PATCH /cursos/69/cupos con cupos=80 | 400 | Problem Details con errores.cupos; PostgreSQL conservó 35. |
| R3 | GET /cursos?creditosMin=5 | 200 | Un solo curso: SIS-310, con 5 créditos. |
| R4 | DELETE /cursos/70 dos veces | 204 y 204 | RET-002 se conservó en la base con activo=false. |
| R5 | GET /cursos/70 | 200 | Se recibió el registro conservado e inactivo. |

Las peticiones adicionales preparan los dos cursos, comprueban que el inactivo desaparezca del listado y desactivan RET-001 al finalizar. Los dos cursos temporales quedaron inactivos. Las filas existentes antes de esta ejecución no cambiaron; los tres ejemplos permanecen activos.

La lógica del reto se había incorporado al seguir el servicio y el controlador completos mostrados en los pasos 11 y 13. Este paso reúne su verificación final; no se añadió una segunda implementación ni se cambió código funcional.

## Archivos

- postman/academia-guia2-reto.postman_collection.json: colección importable, ubicada desde la raíz del proyecto.
- resultado-newman.json y resultado-newman.xml: reportes originales de la ejecución.
- ejecucion-newman.txt: salida de Newman.
- verificaciones-postgresql.json: estado de los cursos después de cada petición.
- resumen.json y variables-ejecucion.json: resumen y códigos utilizados.
- ejecutar-reto.cjs: ejecutor con selección automática de códigos libres y comparación de la base.

Para repetir desde la raíz del proyecto, con API y PostgreSQL iniciados:

```sh
npm install --prefix /tmp/academia-newman --no-save --package-lock=false newman@6.2.1
NODE_PATH=/tmp/academia-newman/node_modules node evidencias/paso-15/ejecutar-reto.cjs
```

La repetición sobrescribe los reportes de esta carpeta y añade dos cursos de prueba que quedan inactivos; respaldar los reportes antes de repetir si se desea mantener cada ejecución. Desde Postman, elegir dos códigos libres en codigoCupos y codigoEliminar; los ids se guardan a partir de POST.

## Cobertura de la guía

Los 14 casos de pruebas generales y los 5 del reto están documentados en dos ejecuciones separadas. Con preparación, límites adicionales y cierre, se realizaron 45 peticiones y 72 aserciones Newman en total. Estas cifras no deben confundirse con las 19 pruebas Java, que son una batería independiente.
