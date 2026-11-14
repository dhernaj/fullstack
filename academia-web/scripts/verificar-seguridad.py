"""Comprobaciones locales del JWT y de la autorización real, sin registrar secretos."""
import base64
import hashlib
import hmac
import json
import os
import time
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = os.environ.get('LAB_API_URL', 'http://localhost:8080/api/v1')
ORIGEN = 'http://localhost:4200'
resultados = []


def consultar(ruta, metodo='GET', datos=None, headers=None):
    encabezados = {'Origin': ORIGEN, **(headers or {})}
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    if cuerpo is not None:
        encabezados['Content-Type'] = 'application/json'
    try:
        respuesta = urlopen(Request(BASE + ruta, data=cuerpo, method=metodo, headers=encabezados), timeout=15)
    except HTTPError as error:
        respuesta = error
    with respuesta:
        crudo = respuesta.read()
        contenido = json.loads(crudo) if crudo and 'json' in respuesta.headers.get('Content-Type', '') else None
        return respuesta.status, respuesta.headers, contenido


def verificar(nombre, condicion):
    if not condicion:
        raise RuntimeError('Falló: ' + nombre)
    resultados.append({'comprobacion': nombre, 'resultado': 'OK'})


def codificar(datos):
    return base64.urlsafe_b64encode(json.dumps(datos, separators=(',', ':')).encode()).rstrip(b'=').decode()


def firmado(claims):
    mensaje = codificar({'alg': 'HS256', 'typ': 'JWT'}) + '.' + codificar(claims)
    firma = hmac.new(base64.b64decode(os.environ['JWT_SECRET'], validate=True), mensaje.encode(), hashlib.sha256).digest()
    return mensaje + '.' + base64.urlsafe_b64encode(firma).rstrip(b'=').decode()


tokens = {}
for usuario in ['admin', 'docente', 'estudiante']:
    password = os.environ['LAB_' + usuario.upper() + '_PASSWORD']
    estado, headers, datos = consultar('/auth/login', 'POST', {'username': usuario, 'password': password})
    verificar('Login de ' + usuario, estado == 200 and datos['tokenType'] == 'Bearer')
    verificar('Login de ' + usuario + ' sin cookie de sesión', headers.get('Set-Cookie') is None)
    tokens[usuario] = datos['accessToken']
    estado, _, pagina = consultar('/cursos', headers={'Authorization': 'Bearer ' + tokens[usuario]})
    verificar('Lectura de cursos como ' + usuario, estado == 200)
    if usuario == 'admin':
        antes = pagina['totalElementos']
        curso_id = pagina['contenido'][0]['id']

solicitud = {'codigo': 'SIS-999', 'nombre': 'No debe crearse', 'creditos': 3, 'docente': 'Prueba de permisos', 'cupos': 30}
for usuario in ['estudiante', 'docente']:
    for metodo, ruta, datos in [
        ('POST', '/cursos', solicitud),
        ('PUT', f'/cursos/{curso_id}', solicitud),
        ('DELETE', f'/cursos/{curso_id}', None),
    ]:
        estado, _, _ = consultar(ruta, metodo, datos, {'Authorization': 'Bearer ' + tokens[usuario]})
        verificar(metodo + ' de cursos rechazado a ' + usuario, estado == 403)

estado, _, _ = consultar('/cursos')
verificar('Sin token', estado == 401)
estado, _, _ = consultar('/cursos', headers={'Cookie': 'accessToken=' + tokens['admin']})
verificar('Token en cookie no autentica', estado == 401)
estado, _, _ = consultar('/cursos', headers={'Authorization': 'Basic ' + base64.b64encode(b'admin:incorrecta').decode()})
verificar('Basic no autentica', estado == 401)

original = tokens['admin'].split('.')
manipulado = '.'.join([original[0], codificar({'sub': 'admin', 'roles': ['ADMIN']}), original[2]])
claims = {'sub': 'admin', 'roles': ['ADMIN'], 'iss': 'academia-api', 'iat': int(time.time()) - 600, 'exp': int(time.time()) - 300}
for nombre, token, esperado in [
    ('JWT firmado vigente de control', firmado({**claims, 'exp': int(time.time()) + 60}), 200),
    ('Firma alterada', manipulado, 401),
    ('Algoritmo none', codificar({'alg': 'none'}) + '.' + codificar(claims) + '.', 401),
    ('JWT firmado pero vencido', firmado(claims), 401),
    ('JWT firmado con emisor incorrecto', firmado({**claims, 'iss': 'otro-emisor', 'exp': int(time.time()) + 60}), 401),
]:
    estado, _, _ = consultar('/cursos', headers={'Authorization': 'Bearer ' + token})
    verificar(nombre, estado == esperado)

for origen, esperado in [(ORIGEN, 200), ('https://externo.example', 403)]:
    estado, _, _ = consultar('/cursos', 'OPTIONS', headers={
        'Origin': origen, 'Access-Control-Request-Method': 'GET', 'Access-Control-Request-Headers': 'authorization'})
    verificar('Preflight de ' + origen, estado == esperado)

estado, _, pagina = consultar('/cursos', headers={'Authorization': 'Bearer ' + tokens['admin']})
verificar('Los intentos sin permiso no cambiaron el total de cursos', estado == 200 and pagina['totalElementos'] == antes)
Path(os.environ.get('LAB_EVIDENCIA_SEGURIDAD', str(Path(__file__).resolve().parents[1] / 'evidencias/seguridad-http.json'))).write_text(
    json.dumps({'base': BASE, 'comprobaciones': resultados}, ensure_ascii=False, indent=2) + '\n')
print(f'{len(resultados)} comprobaciones de seguridad aprobadas; no se guardaron claves ni tokens.')
