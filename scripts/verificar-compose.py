"""Verifica producción y persistencia en un Compose exclusivo de pruebas."""
import json
import os
import subprocess
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

RAIZ = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:' + os.environ.get('API_PORT', '8080')
RESULTADOS = []


def docker(*args):
    return subprocess.check_output(['docker', 'compose', *args], cwd=RAIZ, text=True).strip()


def sql(consulta):
    return docker('exec', '-T', 'db', 'psql', '-U', 'academia', '-d', 'academia', '-X', '-A', '-t', '-v', 'ON_ERROR_STOP=1', '-c', consulta)


def consultar(ruta, metodo='GET', datos=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = Request(BASE + ruta, data=json.dumps(datos).encode() if datos is not None else None, method=metodo, headers=headers)
    try:
        respuesta = urlopen(request, timeout=30)
    except HTTPError as error:
        respuesta = error
    with respuesta:
        cuerpo = respuesta.read()
        contenido = json.loads(cuerpo) if cuerpo and 'json' in respuesta.headers.get('Content-Type', '') else cuerpo.decode()
        return respuesta.status, respuesta.headers, contenido


def comprobar(nombre, condicion):
    if not condicion:
        raise RuntimeError('Falló: ' + nombre)
    RESULTADOS.append({'comprobacion': nombre, 'resultado': 'OK'})
    print('OK: ' + nombre, flush=True)


def login():
    estado, _, sesion = consultar('/api/v1/auth/login', 'POST', {'username': 'admin', 'password': os.environ['ADMIN_PASSWORD']})
    comprobar('Login admin 200', estado == 200 and sesion.get('tokenType') == 'Bearer')
    return sesion['accessToken']


def estado_bd():
    return json.loads(sql("""SELECT json_build_object(
        'migraciones', (SELECT count(*) FROM flyway_schema_history WHERE success),
        'version', (SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1),
        'usuarios', (SELECT count(*) FROM usuarios),
        'estudiantes', (SELECT count(*) FROM estudiantes),
        'matriculas', (SELECT count(*) FROM matriculas),
        'cursos', (SELECT count(*) FROM cursos),
        'adminBcrypt', (SELECT password LIKE '$2%' FROM usuarios WHERE username='admin'),
        'huellaAdmin', (SELECT md5(password) FROM usuarios WHERE username='admin')
    )"""))


if os.environ.get('COMPOSE_PROJECT_NAME', '').startswith('academia-ci-') is False:
    raise SystemExit('Use un COMPOSE_PROJECT_NAME exclusivo que empiece por academia-ci-. No se reinicia el entorno local.')

salida = RAIZ / 'evidencias/parte-e'
salida.mkdir(parents=True, exist_ok=True)
estado, _, salud = consultar('/actuator/health')
comprobar('Health UP sin información interna', estado == 200 and salud.get('status') == 'UP' and 'components' not in salud and 'details' not in salud)
estado, _, _ = consultar('/api/v1/cursos')
comprobar('Cursos sin token 401', estado == 401)
token = login()
for ruta in ['/swagger-ui.html', '/v3/api-docs']:
    estado, _, _ = consultar(ruta, token=token)
    comprobar(ruta + ' desactivado (404)', estado == 404)
estado, _, _ = consultar('/api/v1/auth/login', 'POST', {'username': 'docente', 'password': 'Docente123!'})
comprobar('Cuenta docente de demostración ausente (401)', estado == 401)
antes = estado_bd()
comprobar('Flyway V1 aplicada una sola vez', antes['migraciones'] == 1 and antes['version'] == '1')
comprobar('Solo admin con BCrypt; sin datos demo', antes['usuarios'] == 1 and antes['adminBcrypt'] and all(antes[k] == 0 for k in ['estudiantes', 'matriculas', 'cursos']))
comprobar('API ejecutada sin root', int(docker('exec', '-T', 'api', 'id', '-u')) != 0)
db_id = docker('ps', '-q', 'db')
puertos = json.loads(subprocess.check_output(['docker', 'inspect', '--format', '{{json .NetworkSettings.Ports}}', db_id], text=True))
comprobar('PostgreSQL sin puertos publicados', not any(puertos.values()))
comprobar('API publicada únicamente en loopback', docker('port', 'api', '8080') == BASE.removeprefix('http://'))
estado, headers, curso = consultar('/api/v1/cursos', 'POST', {
    'codigo': 'DCK-401', 'nombre': 'Curso de prueba de persistencia',
    'creditos': 3, 'docente': 'Prueba Docker', 'cupos': 20,
}, token)
comprobar('Curso creado con 201 y Location', estado == 201 and curso.get('codigo') == 'DCK-401' and headers.get('Location', '').endswith('/' + str(curso['id'])))
# Recrear contenedores manteniendo el volumen: equivale al down/up de la guía.
subprocess.run(['docker', 'compose', 'down'], cwd=RAIZ, check=True)
subprocess.run(['docker', 'compose', 'up', '-d', '--no-build', '--wait', '--wait-timeout', '180'], cwd=RAIZ, check=True)
token = login()
estado, _, recuperado = consultar('/api/v1/cursos/' + str(curso['id']), token=token)
comprobar('Curso persiste tras recrear contenedores', estado == 200 and recuperado == curso)
despues = estado_bd()
comprobar('Administrador y migración se conservan', despues == {**antes, 'cursos': 1})
(salida / 'docker-produccion.json').write_text(json.dumps({
    'entorno': 'Docker Compose en runner Linux de GitHub Actions',
    'commit': os.environ.get('GITHUB_SHA', ''), 'runId': os.environ.get('GITHUB_RUN_ID', ''),
    'comprobaciones': RESULTADOS, 'baseAntes': antes, 'baseDespues': despues,
}, ensure_ascii=False, indent=2) + '\n')
print(f'{len(RESULTADOS)} comprobaciones de Docker aprobadas. No se guardan contraseñas ni tokens.')
