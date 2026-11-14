"""Comprueba el contrato paginado con credenciales temporales de Spring Security."""
from pathlib import Path
import base64, json, os, secrets, subprocess, time, urllib.error, urllib.request
ROOT=Path(__file__).resolve().parents[3]
OUT=Path(__file__).resolve().parent
BASE='http://127.0.0.1:18080/api/v1/cursos'
env=os.environ.copy()
for line in (ROOT/'.env.local').read_text().splitlines():
    if line.startswith('export '):
        key,value=line[7:].split('=',1);env[key]=value
password=secrets.token_hex(24)
env.update(SPRING_PROFILES_ACTIVE='h2',SPRING_SECURITY_USER_NAME='verificacion',
           SPRING_SECURITY_USER_PASSWORD=password)
auth='Basic '+base64.b64encode(('verificacion:'+password).encode()).decode()
resultados=[]
def consultar(query,autorizado=True,esperado=200):
    headers={'Accept':'application/json'}
    if autorizado:headers['Authorization']=auth
    try:r=urllib.request.urlopen(urllib.request.Request(BASE+query,headers=headers),timeout=10)
    except urllib.error.HTTPError as error:r=error
    raw=r.read().decode()
    data=json.loads(raw) if raw else None
    resultados.append({'query':query,'conCredencialesTemporales':autorizado,
                       'status':r.status,'respuesta':data})
    assert r.status==esperado
    return data
with (OUT/'servidor-paginacion.txt').open('w') as log:
    proceso=subprocess.Popen(['java','-jar',str(ROOT/'target/academia-api-0.0.1-SNAPSHOT.jar'),
                              '--server.port=18080'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
    try:
        for _ in range(120):
            if proceso.poll() is not None:raise RuntimeError('No arrancó el servidor')
            try:urllib.request.urlopen(BASE,timeout=1)
            except urllib.error.HTTPError as error:
                if error.code==401:break
                raise
            except urllib.error.URLError:time.sleep(.5)
        else:raise RuntimeError('Tiempo de arranque agotado')
        consultar('',False,401)
        pagina=consultar('?size=100000&sort=creditos,desc')
        assert pagina['tamanio']==50 and pagina['totalElementos']==3
        assert pagina['contenido'][0]['codigo']=='SIS-310'
        filtrada=consultar('?docente=ANA&creditosMin=4')
        assert [c['codigo'] for c in filtrada['contenido']]==['SIS-101']
        segunda=consultar('?page=1&size=1&sort=creditos,desc')
        assert segunda['pagina']==1 and segunda['totalPaginas']==3 and len(segunda['contenido'])==1
        literal=consultar('?docente=%25')
        assert literal['totalElementos']==0
        (OUT/'resultados-paginacion.json').write_text(json.dumps(
            {'peticiones':resultados,'autenticacionUsada':'HTTP Basic temporal solo para esta prueba; JWT pendiente'},
            ensure_ascii=False,indent=2)+'\n')
        print('OK: paginación HTTP, filtros, límite 50 y protección provisional comprobados.')
    finally:
        proceso.terminate();proceso.wait(timeout=30)
