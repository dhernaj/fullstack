# Parte E — formulario reactivo de cursos

Fecha local: 12 de noviembre de 2026. Implementación de los pasos 21–22.

## Cambios

Formulario compartido para alta y edición con las reglas del CursoRequest existente: código AAA-999, nombre obligatorio de hasta 100 caracteres, docente obligatorio de hasta 80, créditos enteros entre 1 y 6 y cupos enteros entre 1 y 60. Nombre y docente no admiten solo espacios y se recortan al guardar.

Los errores 400 se muestran junto al campo correspondiente y el detail de un 409 aparece como alerta. Se evita el doble envío. La lectura de edición se cancela si cambia el id o se abandona el componente; no se muestra un formulario editable mientras se carga o después de una lectura fallida. Se rechazan identificadores inválidos sin consultar la API.

Se registraron /cursos/nuevo y /cursos/:id/editar con authGuard y data.roles ADMIN. El id se enlaza mediante withComponentInputBinding.

Durante la prueba real se detectó y corrigió que Angular puede enlazar volver como undefined al abrir /login sin query params. Se añadió el destino por defecto /cursos y una prueba de regresión.

## Pruebas automáticas

`npm test -- --watch=false`, código de salida 0:

```text
Test Files  8 passed (8)
Tests       90 passed (90)
Start at    22:34:20
Duration    6.72s
```

31 casos adicionales respecto a la parte D: campos vacíos, formato de código, espacios, longitudes, límites y números decimales, creación, PUT de edición, error 400 corregible, 409, fallos de lectura, ids inválidos, cancelación, rutas reales permitidas a ADMIN y rechazadas a DOCENTE/ESTUDIANTE, y el regreso del login sin volver.

Los códigos HTTP 400/404/500 y los valores límite se comprobaron con HttpTestingController. La creación, duplicación y edición también se verificaron en el navegador con la API real.

## Evidencia de navegador y datos modificados

API del Laboratorio 2 con perfil H2 de demostración, inicialmente con tres cursos. No se modificaron los cursos SIS-101, SIS-205 ni SIS-310.

1. Campos vacíos: aparecieron los cinco mensajes de validación. formulario-validaciones.png.
2. Código sis-500: se rechazó el formato, con el resto de campos válidos. formulario-codigo-invalido.png.
3. Se creó un curso nuevo SIS-500, Pruebas de Software, docente Docente de Laboratorio, 3 créditos y 30 cupos. La API asignó id 4 y el listado pasó a cuatro cursos. curso-creado.png.
4. Un segundo intento con SIS-500 mostró “Ya existe un curso con código SIS-500”. No se creó otro registro. formulario-409.png.
5. La edición cargó los valores del id 4. formulario-edicion.png.
6. Se cambió únicamente el nombre del curso nuevo a Pruebas de Software II y se comprobó en el listado. curso-editado.png.
7. Tras el arreglo del login, el ingreso directo como estudiante volvió correctamente al listado. Al abrir /cursos/nuevo fue redirigido a /cursos sin mostrar el formulario. formulario-acceso-estudiante.png.

Las capturas no incluyen contraseñas ni tokens. La pestaña quedó en el listado con sesión estudiante. El backend no recibió cambios de código o configuración.

## Pendiente de la guía

La consulta psql que pide verificar SIS-500 en PostgreSQL no se ha ejecutado: esta prueba utilizó H2 en memoria. El curso de práctica no se presenta como persistido en PostgreSQL y se perderá al reiniciar H2. Esta comprobación de persistencia debe completarse al revisar la integración final.

Sigue la parte F, pasos 23–25: pruebas, compilación y revisión documentada de seguridad y calidad.

## Compilación final

`npm run build`, fuera del sandbox, código de salida 0, sin advertencias:

```text
Initial total: 500.84 kB (transferencia estimada: 96.66 kB)
Lazy curso-form: 7.11 kB
Application bundle generation complete. [4.957 seconds]
```

`git diff --check` pasó. El repositorio del backend quedó sin modificaciones.
