"""Arranca H2 nuevo con datos demo y ejecuta la colección por roles con Newman."""
from pathlib import Path
import json,os,re,subprocess,time,urllib.request,urllib.error
ROOT=Path(__file__).resolve().parents[3]
OUT=Path(__file__).resolve().parent
env=os.environ.copy()
for line in (ROOT/'.env.local').read_text().splitlines():
    if line.startswith('export '):
        key,value=line[7:].split('=',1);env[key]=value
assert env.get('NODE_PATH'), 'Indicar NODE_PATH al directorio de instalación local de Newman'
env.update(SPRING_PROFILES_ACTIVE='h2',APP_DEMO_ENABLED='true')
with (OUT/'servidor-seguridad.txt').open('w') as log:
    proceso=subprocess.Popen(['java','-jar',str(ROOT/'target/academia-api-0.0.1-SNAPSHOT.jar'),
        '--server.port=18080'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
    try:
        for _ in range(120):
            if proceso.poll() is not None:raise RuntimeError('No arrancó el servidor; revisar log')
            req=urllib.request.Request('http://127.0.0.1:18080/api/v1/auth/login',method='POST',
                data=json.dumps({'username':'admin','password':env['LAB_ADMIN_PASSWORD']}).encode(),
                headers={'Content-Type':'application/json'})
            try:
                with urllib.request.urlopen(req,timeout=2) as response:
                    if response.status==200:break
            except (urllib.error.URLError,TimeoutError):time.sleep(.5)
        else:raise RuntimeError('No se completó la preparación de usuarios')
        result=subprocess.run(['node',str(OUT/'ejecutar-coleccion.cjs')],cwd=ROOT,env=env,
            text=True,capture_output=True)
        text=result.stdout+'\n'+result.stderr
        for key in ['JWT_SECRET','DB_PASSWORD','LAB_ADMIN_PASSWORD','LAB_DOCENTE_PASSWORD','LAB_ESTUDIANTE_PASSWORD']:
            text=text.replace(env[key],'[omitido]')
        text=re.sub(r'eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[token omitido]',text)
        (OUT/'ejecucion-newman.txt').write_text(text)
        if result.returncode:raise RuntimeError('Falló la colección; revisar reporte depurado')
        print('Colección de seguridad aprobada en H2; reportes sin credenciales.')
    finally:
        proceso.terminate();proceso.wait(timeout=30)
