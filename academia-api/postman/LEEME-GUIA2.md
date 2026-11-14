# Pruebas completas de la guía 2

Importar academia-guia2-pruebas.postman_collection.json en Postman. La colección usa baseUrl=http://127.0.0.1:8080/api/v1 y requiere la API iniciada. Ejecutar las carpetas completas en su orden con Collection Runner.

Los códigos PRB-001 a PRB-007 ya se utilizaron y permanecen reservados aunque los registros estén inactivos. Antes de repetir desde Postman, asignar siete códigos libres a las variables codigoPrincipal, codigoCredito1, codigoCredito6, codigoCupos60, codigoCupos1, codigoNombre100 y codigoDocente80. Los ids se guardan automáticamente a partir de POST. Mantener idInexistente=9223372036854775807.

Las carpetas 01 a 14 corresponden a la tabla de casos de la guía. Algunos casos se desglosan en dos peticiones para probar ambos límites. Las carpetas adicionales completan los límites y desactivan las altas de prueba. La ejecución crea siete filas nuevas y las conserva inactivas, sin eliminarlas físicamente.

## Ejecución realizada

Se ejecutó con Newman 6.2.1: 35 peticiones y 57 aserciones, sin fallos. No fue una ejecución desde la interfaz gráfica de Postman. El reporte JSON original, el reporte JUnit, las consultas a PostgreSQL y la tabla de resultados están en ../evidencias/pruebas-completas.

Para repetir con selección automática de códigos libres y comprobaciones de PostgreSQL, desde la raíz del proyecto:

```sh
npm install --prefix /tmp/academia-newman --no-save --package-lock=false newman@6.2.1
NODE_PATH=/tmp/academia-newman/node_modules node evidencias/pruebas-completas/ejecutar-coleccion.cjs
```

El ejecutor necesita .env.local y PostgreSQL, y sobrescribe los reportes de esa carpeta con la nueva ejecución. Guardar un respaldo de los reportes antes de repetir si se desea conservar cada ejecución. Cada repetición añade siete cursos inactivos.

Los archivos academia-upn-modulo1.* heredados describen el laboratorio anterior; esta colección y sus reportes corresponden a la segunda guía.

Documentación: https://learning.postman.com/docs/reference/newman-cli/installing-running-newman

## Colección del reto

academia-guia2-reto.postman_collection.json contiene los cinco casos R1–R5 con preparación y cierre. La ejecución guardada en evidencias/paso-15 pasó con 10 peticiones y 15 aserciones. RET-001 y RET-002 ya existen inactivos; elegir códigos nuevos antes de repetir desde Postman. El ejecutor ejecutar-reto.cjs selecciona códigos libres automáticamente.
