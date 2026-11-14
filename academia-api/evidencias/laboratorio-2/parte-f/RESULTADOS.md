# Laboratorio 2 — parte F

Se revisaron los seis hallazgos de la tabla 3 de la guía y la matriz actual de permisos. El informe está en SEGURIDAD.md, en la raíz del proyecto.

## Comprobaciones

Las 21 peticiones de la revisión pasaron. Se utilizaron las cuentas demo existentes de la instancia H2 local y solo operaciones de lectura, login y rechazos previos a escritura. El POST de curso como estudiante se rechazó con 403 y no creó un registro.

- ESTUDIANTE no pudo consultar las matrículas de un curso ni las de otro estudiante; sí pudo consultar las propias.
- DOCENTE pudo consultar las matrículas de curso y ADMIN los estudiantes.
- El tamaño solicitado de 100000 quedó limitado a 50.
- Tokens vencidos, de otro emisor, sin firma y con roles alterados fueron rechazados con 401.
- Basic y cookie con token válido no autenticaron. No se observaron cookies de sesión.
- CORS permitió el origen local definido y rechazó otro origen.
- Usuario desconocido y contraseña incorrecta recibieron el mismo mensaje genérico.
- El secreto JWT no tiene valor por defecto; .env.local está fuera de Git y sus permisos son 600.
- Se revisaron los archivos versionados actuales en busca de los cinco secretos locales. No se inspeccionó todo el historial.
- El catálogo PostgreSQL se comparó antes y después y no cambió.

La revisión no cambió código funcional ni configuración de seguridad. No se repitió la batería Maven, que ya había pasado en la parte E; aquí se añadieron comprobaciones específicas de seguridad y documentación.

## Pendientes de la guía

El login no limita intentos fallidos. El bloqueo para matricular está implementado, pero falta verificar las dos solicitudes simultáneas del reto. Se documentó además la ausencia de revocación de tokens y el alcance de desarrollo del proyecto.

## Repetir

Con la instancia de la aplicación iniciada y los datos demo habilitados:

```sh
python3 evidencias/laboratorio-2/parte-f/verificar_seguridad.py
```

El ejecutor utiliza .env.local y PostgreSQL para comparar el catálogo. LAB_API_URL permite cambiar la URL de la API si se mantiene el mismo conjunto de cuentas y datos. El resultado reemplaza resultados.json; guardar una copia antes de repetir si se quiere conservar cada ejecución.

Siguiente sección: reto, con sus cinco casos y la prueba real de concurrencia del último cupo.
