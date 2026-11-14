"""Comprueba OpenAPI y health en la vista local H2, sin guardar claves ni tokens."""
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = 'http://127.0.0.1:8083'
resultados = []


def consultar(ruta, metodo='GET', datos=None, token=None):
    headers = {}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    if cuerpo is not None:
        headers['Content-Type'] = 'application/json'
    try:
        respuesta = urlopen(Request(BASE + ruta, data=cuerpo, method=metodo, headers=headers), timeout=20)
    except HTTPError as error:
        respuesta = error
    with respuesta:
        crudo = respuesta.read()
        contenido = json.loads(crudo) if crudo and 'json' in respuesta.headers.get('Content-Type', '') else crudo.decode()
        return respuesta.status, respuesta.headers, contenido


def verificar(nombre, condicion):
    if not condicion:
        raise RuntimeError('Falló: ' + nombre)
    resultados.append({'comprobacion': nombre, 'resultado': 'OK'})


estado, _, health = consultar('/actuator/health')
verificar('Health público UP sin detalles internos', estado == 200 and health['status'] == 'UP' and 'components' not in health and 'details' not in health)
estado, _, html = consultar('/swagger-ui.html')
verificar('Swagger UI público', estado == 200 and 'Swagger UI' in html)
estado, _, contrato = consultar('/v3/api-docs')
verificar('OpenAPI público con metadatos', estado == 200 and contrato['info']['title'] == 'Academia UPN API')
verificar('Esquema HTTP Bearer JWT', contrato['components']['securitySchemes']['bearerAuth']['scheme'] == 'bearer')
verificar('Login documentado sin exigir JWT', contrato['paths']['/api/v1/auth/login']['post']['security'] == [])
verificar('Secciones de los cuatro controladores', {'Cursos', 'Estudiantes', 'Matrículas', 'Autenticación'} <= {t['name'] for t in contrato['tags']})
parametros = {p['name'] for p in contrato['paths']['/api/v1/cursos']['get']['parameters']}
verificar('Paginación y filtros documentados', {'page', 'size', 'sort', 'docente', 'creditosMin'} <= parametros)
respuestas = contrato['paths']['/api/v1/cursos']['post']['responses']
verificar('Errores 400 y 409 con ProblemDetail', all(respuestas[c]['content']['application/problem+json']['schema']['$ref'].endswith('/ProblemDetail') for c in ['400', '409']))
estado, _, _ = consultar('/api/v1/cursos')
verificar('Cursos sin JWT devuelve 401', estado == 401)
estado, _, sesion = consultar('/api/v1/auth/login', 'POST', {'username': 'admin', 'password': 'solo-demo-swagger-admin-2026'})
verificar('Login de demostración', estado == 200 and sesion['tokenType'] == 'Bearer')
token = sesion['accessToken']
estado, headers, curso = consultar('/api/v1/cursos', 'POST', {
    'codigo': 'DOC-402', 'nombre': 'Comprobación OpenAPI', 'creditos': 3,
    'docente': 'Docente de demostración', 'cupos': 20,
}, token)
verificar('Alta autorizada 201 con Location', estado == 201 and curso['codigo'] == 'DOC-402' and headers.get('Location', '').endswith('/' + str(curso['id'])))
estado, _, _ = consultar('/actuator/env', token=token)
verificar('Otros endpoints de Actuator permanecen cerrados', estado == 403)
salida = Path(__file__).resolve().parents[1] / 'evidencias/parte-d'
(salida / 'openapi.json').write_text(json.dumps(contrato, ensure_ascii=False, indent=2) + '\n')
(salida / 'http-resumen.json').write_text(json.dumps({
    'instancia': BASE, 'entorno': 'H2 de demostración local, sin PostgreSQL',
    'health': health, 'cursoDePrueba': curso, 'comprobaciones': resultados,
}, ensure_ascii=False, indent=2) + '\n')
print(f'{len(resultados)} comprobaciones aprobadas; contrato guardado sin credenciales ni tokens.')
