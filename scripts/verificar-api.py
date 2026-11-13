"""Comprueba la integración local sin escribir contraseñas ni tokens en la evidencia."""
import json
import os
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = 'http://localhost:8080/api/v1'
ORIGEN = 'http://localhost:4200'
resultados = []


def consultar(ruta, metodo='GET', datos=None, headers=None):
    encabezados = {'Origin': ORIGEN, **(headers or {})}
    cuerpo = None
    if datos is not None:
        cuerpo = json.dumps(datos).encode()
        encabezados['Content-Type'] = 'application/json'
    request = Request(BASE + ruta, data=cuerpo, method=metodo, headers=encabezados)
    try:
        respuesta = urlopen(request, timeout=15)
    except HTTPError as error:
        respuesta = error
    with respuesta:
        crudo = respuesta.read()
        return respuesta.status, respuesta.headers, json.loads(crudo) if crudo else None


def verificar(nombre, condicion):
    if not condicion:
        raise RuntimeError('Falló la comprobación: ' + nombre)
    resultados.append({'comprobacion': nombre, 'resultado': 'OK'})


clave = os.environ.get('LAB_ADMIN_PASSWORD')
if not clave:
    raise RuntimeError('Carga LAB_ADMIN_PASSWORD desde la configuración local antes de ejecutar')

estado, _, _ = consultar('/cursos')
verificar('Sin token: cursos responde 401', estado == 401)
estado, _, _ = consultar('/auth/login', 'POST', {'username': 'admin', 'password': 'clave-incorrecta'})
verificar('Credenciales incorrectas: login responde 401', estado == 401)
estado, headers, sesion = consultar('/auth/login', 'POST', {'username': 'admin', 'password': clave})
verificar('Login correcto: 200 y contrato del Laboratorio 2', estado == 200 and
          set(sesion) == {'accessToken', 'tokenType', 'expiresIn', 'username', 'roles'} and
          sesion['tokenType'] == 'Bearer' and sesion['expiresIn'] > 0 and sesion['username'] == 'admin' and 'ADMIN' in sesion['roles'])
verificar('CORS permite el origen de Angular', headers.get('Access-Control-Allow-Origin') == ORIGEN)
estado, _, pagina = consultar('/cursos', headers={'Authorization': 'Bearer ' + sesion['accessToken']})
verificar('Bearer emitido: cursos responde 200 con paginación', estado == 200 and
          {'contenido', 'pagina', 'tamanio', 'totalElementos', 'totalPaginas', 'ultima'} <= set(pagina))
estado, _, _ = consultar('/cursos', headers={'Authorization': 'Bearer token-invalido'})
verificar('Token inválido: cursos responde 401', estado == 401)
estado, headers, _ = consultar('/cursos', 'OPTIONS', headers={
    'Access-Control-Request-Method': 'GET', 'Access-Control-Request-Headers': 'authorization'})
verificar('Preflight permite Authorization', estado == 200 and
          headers.get('Access-Control-Allow-Origin') == ORIGEN and
          'authorization' in headers.get('Access-Control-Allow-Headers', '').lower())

salida = Path(__file__).resolve().parents[1] / 'evidencias/api-parte-b.json'
salida.write_text(json.dumps({'entorno': 'API local con perfil H2 de demostración',
                             'comprobaciones': resultados}, ensure_ascii=False, indent=2) + '\n')
print(f'{len(resultados)} comprobaciones HTTP aprobadas. Evidencia sin credenciales ni tokens.')
