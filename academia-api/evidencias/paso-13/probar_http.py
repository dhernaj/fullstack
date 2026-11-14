"""Comprueba el controlador del paso 13 con PostgreSQL real."""
from pathlib import Path
import json, os, subprocess, time, urllib.request, urllib.error
ROOT=Path(__file__).resolve().parents[2]
OUT=Path(__file__).resolve().parent
BASE='http://127.0.0.1:18080/api/v1/cursos'
env=os.environ.copy()
for line in (ROOT/'.env.local').read_text().splitlines():
    if line.startswith('export '):
        key,value=line[7:].split('=',1);env[key]=value
resultados=[]
def pedir(metodo,ruta='',body=None,esperado=200):
    data=json.dumps(body).encode() if body is not None else None
    req=urllib.request.Request(BASE+ruta,data=data,method=metodo,headers={'Content-Type':'application/json'})
    try: response=urllib.request.urlopen(req,timeout=10)
    except urllib.error.HTTPError as error: response=error
    raw=response.read().decode()
    respuesta=json.loads(raw) if raw else None
    resultados.append({'metodo':metodo,'ruta':ruta or '/', 'esperado':esperado,
                       'obtenido':response.status,'respuesta':respuesta,
                       'location':response.headers.get('Location')})
    assert response.status==esperado, resultados[-1]
    return respuesta,response.headers

with (OUT/'servidor-http.txt').open('w') as log:
    proceso=subprocess.Popen(['java','-jar',str(ROOT/'target/academia-api-0.0.1-SNAPSHOT.jar'),
                              '--server.port=18080'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
    try:
        for _ in range(120):
            if proceso.poll() is not None:raise RuntimeError('Falló el arranque')
            try:
                urllib.request.urlopen(BASE,timeout=1).close();break
            except urllib.error.URLError:time.sleep(.5)
        else:raise RuntimeError('Tiempo de arranque agotado')
        iniciales,_=pedir('GET')
        consulta=subprocess.run([
            '/usr/local/opt/postgresql@17/bin/psql', '-h', '127.0.0.1',
            '-U', env['DB_USERNAME'], '-d', 'academia_db', '-At', '-v', 'ON_ERROR_STOP=1',
            '-c', "SELECT EXISTS (SELECT 1 FROM cursos WHERE codigo='SIS-400')"
        ], env=dict(env, PGPASSWORD=env['DB_PASSWORD']), capture_output=True, text=True, check=True)
        assert consulta.stdout.strip()=='f', 'SIS-400 ya existe (activo o inactivo); no se modifica'
        pedir('GET','/'+str(iniciales[0]['id']))
        datos={'codigo':'SIS-400','nombre':'Arquitectura de Software','creditos':4,
               'docente':'José Canchucaja','cupos':25,'id':999,'activo':False}
        creado,headers=pedir('POST',body=datos,esperado=201)
        ruta='/'+str(creado['id'])
        assert headers['Location'].endswith(ruta) and creado['activo'] and creado['id']!=999
        datos['cupos']=30
        actualizado,_=pedir('PUT',ruta,datos)
        assert actualizado['cupos']==30
        parcial,_=pedir('PATCH',ruta+'/cupos',{'cupos':35})
        assert parcial==dict(actualizado,cupos=35)
        pedir('PATCH',ruta+'/cupos',{'cupos':80},esperado=400)
        pedir('POST',body=dict(datos,codigo='sis400'),esperado=400)
        filtrados,_=pedir('GET','?docente=Canchucaja&creditosMin=4')
        assert [c['id'] for c in filtrados]==[creado['id']]
        pedir('GET','/abc',esperado=400)
        pedir('DELETE',ruta,esperado=204)
        pedir('DELETE',ruta,esperado=204)
        inactivo,_=pedir('GET',ruta)
        assert not inactivo['activo']
        finales,_=pedir('GET')
        assert finales==iniciales
        print(f'OK: {len(resultados)} peticiones; SIS-400 conservado inactivo, ejemplos sin cambios.')
    finally:
        (OUT/'resultados-http.json').write_text(json.dumps(resultados,ensure_ascii=False,indent=2)+'\n')
        proceso.terminate();proceso.wait(timeout=30)
