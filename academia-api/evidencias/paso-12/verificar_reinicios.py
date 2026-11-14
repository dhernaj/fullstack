"""Compara los cursos después del arranque y de dos reinicios completos."""
from pathlib import Path
import json
import os
import subprocess
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
EVIDENCIA = Path(__file__).resolve().parent
env = os.environ.copy()
for line in (ROOT / '.env.local').read_text().splitlines():
    if line.startswith('export '):
        key, value = line[7:].split('=', 1)
        env[key] = value

def cursos():
    db_env = dict(env, PGPASSWORD=env['DB_PASSWORD'], PGCLIENTENCODING='UTF8')
    result = subprocess.run([
        '/usr/local/opt/postgresql@17/bin/psql', '-h', '127.0.0.1',
        '-U', env['DB_USERNAME'], '-d', 'academia_db', '-At', '-v', 'ON_ERROR_STOP=1',
        '-c', 'SELECT row_to_json(c) FROM (SELECT * FROM cursos ORDER BY id) c'
    ], env=db_env, capture_output=True, text=True, check=True)
    return [json.loads(line) for line in result.stdout.splitlines()]

inicial = cursos()
assert {c['codigo'] for c in inicial} == {'SIS-101', 'SIS-205', 'SIS-310'}
assert len(inicial) == 3
assert next(c for c in inicial if c['codigo'] == 'SIS-101')['nombre'] == 'Fundamentos de Programación'
assert next(c for c in inicial if c['codigo'] == 'SIS-205')['docente'] == 'Luis Pérez'
resultados = []
for numero in range(1, 4):
    with (EVIDENCIA / f'arranque-{numero}.txt').open('w') as log:
        proceso = subprocess.Popen([
            'java', '-jar', str(ROOT / 'target/academia-api-0.0.1-SNAPSHOT.jar'),
            '--server.port=18080'
        ], cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            disponible = False
            for _ in range(120):
                if proceso.poll() is not None:
                    raise RuntimeError('Falló el arranque; revisar evidencia')
                try:
                    urllib.request.urlopen('http://127.0.0.1:18080/api/v1/cursos', timeout=1)
                except urllib.error.HTTPError as error:
                    assert error.code == 404
                    disponible = True
                    break
                except urllib.error.URLError:
                    time.sleep(.5)
            assert disponible, 'Tiempo de arranque agotado'
            actuales = cursos()
            assert actuales == inicial, 'Los datos cambiaron después del reinicio'
            resultados.append({'arranque': numero, 'reinicio': numero - 1,
                               'filas': len(actuales), 'datos_e_ids_conservados': True})
        finally:
            proceso.terminate()
            proceso.wait(timeout=30)
(EVIDENCIA / 'reinicios.json').write_text(json.dumps(
    {'cursos': inicial, 'verificaciones': resultados}, ensure_ascii=False, indent=2) + '\n')
print('OK: tres arranques, dos reinicios, tres cursos sin duplicados y mismos identificadores.')
