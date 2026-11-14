"""Comprueba el perfil prod local sin registrar contraseñas ni tokens."""
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

RAIZ = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:8084'
secretos = dict(linea.split('=', 1) for linea in (RAIZ / '.env').read_text().splitlines() if linea and not linea.startswith('#'))
resultados = []

def consultar(ruta, metodo='GET', datos=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    solicitud = Request(BASE + ruta, data=json.dumps(datos).encode() if datos is not None else None, method=metodo, headers=headers)
    try:
        respuesta = urlopen(solicitud, timeout=20)
    except HTTPError as error:
        respuesta = error
    with respuesta:
        cuerpo = respuesta.read()
        return respuesta.status, json.loads(cuerpo) if cuerpo and 'json' in respuesta.headers.get('Content-Type', '') else cuerpo.decode()

def comprobar(nombre, condicion):
    if not condicion:
        raise RuntimeError('Falló: ' + nombre)
    resultados.append({'comprobacion': nombre, 'resultado': 'OK'})

estado, salud = consultar('/actuator/health')
comprobar('Health público UP', estado == 200 and salud['status'] == 'UP')
comprobar('Health sin detalles internos', 'components' not in salud and 'details' not in salud)
estado, _ = consultar('/api/v1/cursos')
comprobar('Cursos requieren autenticación', estado == 401)
estado, sesion = consultar('/api/v1/auth/login', 'POST', {'username': 'admin', 'password': secretos['ADMIN_PASSWORD']})
comprobar('Administrador de producción puede ingresar', estado == 200 and sesion['tokenType'] == 'Bearer')
token = sesion['accessToken']
for ruta in ['/swagger-ui.html', '/v3/api-docs']:
    estado, _ = consultar(ruta, token=token)
    comprobar(ruta + ' desactivado en producción', estado == 404)
estado, cursos = consultar('/api/v1/cursos', token=token)
comprobar('No se cargan cursos demo', estado == 200 and all(curso['codigo'] == 'PRD-401' for curso in cursos['contenido']))
if cursos['totalElementos'] == 0:
    estado, curso = consultar('/api/v1/cursos', 'POST', {'codigo': 'PRD-401', 'nombre': 'Prueba de producción local', 'creditos': 3, 'docente': 'Docente de prueba', 'cupos': 20}, token)
    comprobar('Alta de curso en PostgreSQL', estado == 201 and curso['codigo'] == 'PRD-401')
else:
    comprobar('Curso conservado tras reiniciar', cursos['totalElementos'] == 1)
estado, _ = consultar('/api/v1/auth/login', 'POST', {'username': 'docente', 'password': 'solo-demo-swagger-docente-2026'})
comprobar('Cuenta docente de demostración ausente', estado == 401)
salida = RAIZ / 'evidencias/parte-e'
(salida / 'http-prod-local.json').write_text(json.dumps({'instancia': BASE, 'entorno': 'PostgreSQL nativo; esquema laboratorio4_prod_prueba; NO es una prueba de Docker', 'comprobaciones': resultados}, ensure_ascii=False, indent=2) + '\n')
print(f'{len(resultados)} comprobaciones aprobadas, sin guardar credenciales ni tokens.')
