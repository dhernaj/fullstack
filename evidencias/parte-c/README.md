# Módulo IV — Parte C: pruebas del frontend

Fecha local: 13 de noviembre de 2026. Pasos 11–13 de la guía.

## Resultado

npm test -- --watch=false, equivalente a ng test --watch=false, terminó con código de salida 0:

```text
Test Files  9 passed (9)
Tests       97 passed (97)
Start at    21:44:31
Duration    24.46s
```

La compilación de pruebas terminó en 12.419 segundos. La salida real completa está en vitest.txt. La hora UTC de compilación cae en el 14 de noviembre; la ejecución corresponde al 13 de noviembre en America/Lima.

## Paso 11 — CursoService

Se conservó curso.service.spec.ts y se reforzó con comprobación de GET y de la respuesta paginada completa que recibe el suscriptor. Se añadió el caso de filtro formado solo por espacios. Los siete casos del archivo comprueban:

- Parámetros y orden predeterminados, sin filtro vacío.
- Página, tamaño, orden y docente recortado, conservando la respuesta de la API.
- Ausencia del parámetro docente cuando solo contiene espacios.
- Lectura de un curso por id.
- POST y PUT con el cuerpo esperado.
- DELETE y respuesta 204 sin contenido.

Los fixtures incluyen ultima, obligatorio en nuestro contrato Pagina. No se alteró el modelo ni la API para copiar el ejemplo simplificado de la guía.

## Paso 12 — interceptor JWT

Se creó auth.interceptor.spec.ts con AuthService sustituido por un doble de prueba, como pide la guía. Sus tres casos comprueban:

1. Se adjunta Bearer a nuestra API y se conservan las otras cabeceras.
2. El token no se envía a otro dominio y un 401 externo no llama a logout.
3. Un 401 de nuestra API para el token actual llama a logout.

Cada prueba termina con HttpTestingController.verify para detectar peticiones sin atender. Las pruebas anteriores de sesión e interceptor con el AuthService real, incluidas expiración, restauración y 401 atrasado, se conservaron en auth.spec.ts.

## Paso 13 — suite completa

Se conservaron las 93 pruebas anteriores y se añadieron cuatro: filtro compuesto solo por espacios y tres pruebas aisladas del interceptor. La suite completa ejecutó nueve archivos y 97 casos. El ejemplo de la guía suma nueve pruebas porque parte de proyectos sin las pruebas que ya teníamos; no se eliminaron casos para igualar ese número.

Se usaron las dependencias instaladas desde package-lock.json en la parte A. No se ejecutó una nueva instalación ni se modificaron package.json o el lockfile. Vitest reportó versión 5.1.0.

La suite utiliza provideHttpClientTesting y respuestas simuladas, sin necesitar API, PostgreSQL, contraseñas o tokens reales. Es una verificación del frontend en el entorno de test; las pruebas E2E con navegador y API corresponden a la parte F.

## Alcance

Solo cambiaron pruebas y documentación. El código de funcionamiento de Angular y el backend no se modificaron en esta parte. Los proyectos originales de los laboratorios 2 y 3 permanecieron intactos.

Sigue la parte D, pasos 14–17: OpenAPI, Swagger UI con JWT y Actuator health.
