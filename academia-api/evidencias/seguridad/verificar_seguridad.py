"""Comprobaciones locales de seguridad del laboratorio; no es una auditoría integral."""
from pathlib import Path
import json, os, subprocess, urllib.request, urllib.error, urllib.parse
ROOT=Path(__file__).resolve().parents[2]
OUT=Path(__file__).resolve().parent
BASE='http://127.0.0.1:8080/api/v1/cursos'
env=os.environ.copy()
for line in (ROOT/'.env.local').read_text().splitlines():
    if line.startswith('export '):
        key,value=line[7:].split('=',1);env[key]=value

def sql(query):
    return subprocess.run(['/usr/local/opt/postgresql@17/bin/psql','-h','127.0.0.1',
        '-U',env['DB_USERNAME'],'-d','academia_db','-At','-v','ON_ERROR_STOP=1','-c',query],
        env=dict(env,PGPASSWORD=env['DB_PASSWORD'],PGCLIENTENCODING='UTF8'),
        capture_output=True,text=True,check=True).stdout.strip()

antes=json.loads(sql("SELECT coalesce(json_agg(c),'[]'::json) FROM (SELECT * FROM cursos ORDER BY id) c"))
used={c['codigo'] for c in antes}
codigo=next(f'SEC-{i:03d}' for i in range(1,1000) if f'SEC-{i:03d}' not in used)
results=[]
def http(method,path='',body=None,status=200):
    req=urllib.request.Request(BASE+path,method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={'Content-Type':'application/json'})
    try:r=urllib.request.urlopen(req,timeout=10)
    except urllib.error.HTTPError as e:r=e
    raw=r.read().decode();data=json.loads(raw) if raw else None
    results.append({'metodo':method,'ruta':path or '/','esperado':status,'obtenido':r.status,'respuesta':data})
    assert r.status==status, results[-1]
    return data

body={'codigo':codigo,'nombre':'Comprobación de seguridad','creditos':4,
      'docente':'Prueba local','cupos':25,'id':999,'activo':False}
created=http('POST',body=body,status=201)
try:
    assert created['id']!=999 and created['activo'] is True
    # La petición no envía credenciales: su éxito confirma el pendiente de autenticación.
    http('POST',body=dict(body,nombre='A'*5000),status=400)
    error=http('GET','/abc',status=400)
    raw=json.dumps(error)
    assert not any(token in raw for token in ['stackTrace','org.springframework','java.lang','"trace"','"exception"'])
    for text in ['.*', "' OR 1=1 --"]:
        assert http('GET','?'+urllib.parse.urlencode({'docente':text}))==[]
finally:
    http('DELETE','/'+str(created['id']),status=204)

despues=json.loads(sql("SELECT coalesce(json_agg(c),'[]'::json) FROM (SELECT * FROM cursos ORDER BY id) c"))
assert [c for c in despues if c['id']!=created['id']]==antes
assert next(c for c in despues if c['id']==created['id'])['activo'] is False
role=json.loads(sql("SELECT row_to_json(r) FROM (SELECT current_user AS usuario, rolsuper, rolcreatedb, rolcreaterole, rolreplication, rolbypassrls FROM pg_roles WHERE rolname=current_user) r"))
assert all(role[k] is False for k in ['rolsuper','rolcreatedb','rolcreaterole','rolreplication','rolbypassrls'])
source=ROOT/'src/main/java'
assert not list(source.rglob('*Inseguro*'))
controller=(source/'pe/edu/upn/academia/controller/CursoController.java').read_text()
service=(source/'pe/edu/upn/academia/service/CursoService.java').read_text()
assert 'CursoRepository' not in controller and '@RequestBody Curso ' not in controller
assert 'matches(' not in service and 'contains(' in service
tracked=subprocess.run(['git','ls-files'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.splitlines()
assert '.env.local' not in tracked
for name in tracked:
    path=ROOT/name
    if path.is_file():assert env['DB_PASSWORD'].encode() not in path.read_bytes(), 'Credencial en un archivo versionado'
report={'comprobaciones_http':results,'asignacion_masiva_bloqueada':True,
        'curso_temporal':{'id':created['id'],'codigo':codigo,'estado_final':'inactivo'},
        'originales_conservados':True,'autenticacion':'pendiente: POST sin credenciales respondió 201',
        'rol_postgresql':role,'controlador_inseguro_ausente':True,
        'secreto_local_no_versionado':True,'filtro_sin_regex_usuario':True}
(OUT/'resultados.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('OK: protecciones verificadas, datos anteriores conservados; autenticación pendiente confirmada.')
