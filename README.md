# Academia web — Laboratorio 3

Frontend Angular para la API de los laboratorios anteriores.

## Desarrollo en macOS

```sh
npm ci
npm start -- --host localhost
```

Abrir http://localhost:4200. La API del Laboratorio 2 debe ejecutarse en http://localhost:8080 para las partes B–F.

```sh
npm test -- --watch=false
npm run build
```

## Parte A — pasos 1 a 6

Proyecto standalone sin SSR, Bootstrap 5.3, estilos del curso, idioma español y componente inicial con RouterOutlet.

Se conserva Node.js 24.21.0 instalado en la Mac Intel: es compatible con Angular 22. El proyecto se generó con Angular CLI 22.2.6 y npm resolvió CLI 22.2.7 / Angular 22.2.6; se usa desde npm, sin instalación global. Las versiones resueltas quedan en package-lock.json.

El entorno de desarrollo apunta a http://localhost:8080/api/v1. Producción usa /api/v1 como ruta del mismo dominio hasta configurar el despliegue en el Módulo IV; no se utiliza el dominio de ejemplo de la guía como si estuviera desplegado. Los entornos no contienen contraseñas ni secretos JWT.

Se mantendrá un commit por parte A–F. La parte B incorporará modelos, sesión, interceptor y login.
