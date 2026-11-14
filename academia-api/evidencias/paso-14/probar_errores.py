"""Verifica errores HTTP sin modificar los cursos existentes."""
from pathlib import Path
import json, os, subprocess, time, urllib.error, urllib.request
ROOT=Path(__file__).resolve().parents[2]
OUT=Path(__file__).resolve().parent
BASE='http://127.0.0.1:18080/api/v1/cursos'
env=os.environ.copy()
for line in (ROOT/'.env.local').read_text().splitlines():
    if line.startswith('export '):
        key,value=line[7:].split('=',1);env[key]=value

def snapshot():
    return subprocess.run([
        '/usr/local/opt/postgresql@17/bin/psql','-h','127.0.0.1','-U',env['DB_USERNAME'],
        '-d','academia_db','-At','-v','ON_ERROR_STOP=1','-c',
        'SELECT row_to_json(c) FROM (SELECT * FROM cursos ORDER BY id) c'
    ],env=dict(env,PGPASSWORD=env['DB_PASSWORD'],PGCLIENTENCODING='UTF8'),
      capture_output=True,text=True,check=True).stdout

resultados=[]
def pedir(metodo,ruta='',body=None,esperado=400,tipo='application/json'):
    data=body.encode() if isinstance(body,str) else (json.dumps(body).encode() if body is not None else None)
    req=urllib.request.Request(BASE+ruta,data=data,method=metodo,headers={'Content-Type':tipo})
    try:response=urllib.request.urlopen(req,timeout=10)
    except urllib.error.HTTPError as error:response=error
    raw=response.read().decode();respuesta=json.loads(raw)
    resultado={'metodo':metodo,'ruta':ruta or '/','esperado':esperado,'obtenido':response.status,
               'content_type':response.headers.get('Content-Type'),'respuesta':respuesta}
    resultados.append(resultado)
    assert response.status==esperado,resultado
    assert response.headers.get('Content-Type').startswith('application/problem+json'),resultado
    assert respuesta['status']==esperado and respuesta['title'] and respuesta['instance']
    assert not any(token in raw for token in ['stackTrace','SQLException','org.springframework','java.lang','"trace"','"exception"'])
    return respuesta

antes=snapshot()
with (OUT/'servidor-errores.txt').open('w') as log:
    proceso=subprocess.Popen(['java','-jar',str(ROOT/'target/academia-api-0.0.1-SNAPSHOT.jar'),
                              '--server.port=18080'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
    try:
        for _ in range(120):
            if proceso.poll() is not None:raise RuntimeError('Falló el arranque')
            try:urllib.request.urlopen(BASE,timeout=1).close();break
            except urllib.error.URLError:time.sleep(.5)
        else:raise RuntimeError('Tiempo agotado')
        datos={'codigo':'SIS-101','nombre':'Curso de prueba','creditos':4,'docente':'Ana','cupos':30}
        inexistente='/9223372036854775807'
        pedir('GET',inexistente,esperado=404)
        pedir('PUT',inexistente,datos,esperado=404)
        pedir('PATCH',inexistente+'/cupos',{'cupos':35},esperado=404)
        pedir('DELETE',inexistente,esperado=404)
        pedir('POST',body=datos,esperado=409)
        invalidos=pedir('POST',body=dict(datos,nombre='',creditos=9))
        assert set(invalidos['errores'])=={'nombre','creditos'}
        pedir('POST',body=dict(datos,codigo='sis400'))
        pedir('POST',body='{')
        pedir('GET','/abc')
        pedir('GET','?creditosMin=abc')
        with urllib.request.urlopen(BASE,timeout=5) as response:curso=json.load(response)[0]
        pedir('PATCH','/'+str(curso['id'])+'/cupos',{'cupos':80})
        pedir('PATCH',body={'cupos':35},esperado=405)
        pedir('POST',body=json.dumps(datos),esperado=415,tipo='text/plain')
        pedir('POST',body=dict(datos,nombre='A'*101))
        assert snapshot()==antes,'Los datos cambiaron durante las pruebas de error'
        (OUT/'resultados-errores.json').write_text(json.dumps(
            {'peticiones':resultados,'datos_conservados':True},ensure_ascii=False,indent=2)+'\n')
        print(f'OK: {len(resultados)} respuestas de error verificadas, sin cambios en los cursos.')
    finally:
        proceso.terminate();proceso.wait(timeout=30)
