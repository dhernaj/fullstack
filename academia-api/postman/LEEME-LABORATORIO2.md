# Pruebas de seguridad del Laboratorio 2

La colección academia-lab2-seguridad.postman_collection.json contiene los doce casos de la parte E de la guía, tres logins y comprobaciones adicionales. Se ejecutaron 28 peticiones y 39 aserciones con Newman 6.2.1, sin fallos. El reporte se depuró para omitir contraseñas y tokens; los estados y resultados de las aserciones se conservaron.

## Ejecutar desde Postman

1. Iniciar H2 nuevo con ./iniciar-macos.command (APP_DEMO_ENABLED=true por defecto en el script).
2. Importar la colección.
3. Completar adminPassword, docentePassword y estudiantePassword con los valores locales de LAB_ADMIN_PASSWORD, LAB_DOCENTE_PASSWORD y LAB_ESTUDIANTE_PASSWORD en .env.local. Mantener esos valores locales; no exportarlos ni compartirlos.
4. Ejecutar todas las carpetas en orden. Los logins guardan tokenAdmin, tokenDocente y tokenEstudiante para las peticiones siguientes.

El caso de creación usa SEG-001. Para repetir, reiniciar H2 desde cero o adaptar el código de prueba y la preparación. La colección espera los datos demo iniciales; no ejecutarla contra PostgreSQL persistente sin revisar esas condiciones.

## Ejecución reproducible con Newman

Desde la raíz del proyecto:

```sh
npm install --prefix /tmp/academia-newman --no-save --package-lock=false newman@6.2.1
NODE_PATH=/tmp/academia-newman/node_modules python3 evidencias/laboratorio-2/parte-e/probar_seguridad.py
```

El ejecutor inicia H2 temporal en el puerto 18080, espera a que estén listas las cuentas demo y ejecuta la colección. Al terminar detiene el proceso y su base se descarta. Necesita el ejecutable empaquetado, .env.local y el puerto libre.

Los reportes de esa carpeta se sobrescriben al repetir. El reporte temporal sin depurar se elimina después de guardar el resultado sin contraseñas ni tokens. No compartir reportes sin depurar.
