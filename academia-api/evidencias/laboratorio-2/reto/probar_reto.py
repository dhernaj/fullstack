"""Cinco casos del reto en H2 y en un esquema PostgreSQL temporal aislado."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import base64, json, os, re, subprocess, threading, time, urllib.error, urllib.request, uuid
ROOT=Path(__file__).resolve().parents[3]
OUT=Path(__file__).resolve().parent
BASE='http://127.0.0.1:18080/api/v1'
variables=dict(line[7:].split('=',1) for line in (ROOT/'.env.local').read_text().splitlines() if line.startswith('export '))
pg_env=dict(os.environ,PGPASSWORD=variables['DB_PASSWORD'],PGCLIENTENCODING='UTF8',
            PGCONNECT_TIMEOUT='5',PGAPPNAME='lab2-reto-verificacion')
PG=['/usr/local/opt/postgresql@17/bin/psql','-h','127.0.0.1','-U',variables['DB_USERNAME'],
    '-d','academia_db','-At','-v','ON_ERROR_STOP=1']

def sql(query):
    return subprocess.check_output(PG+['-c',query],env=pg_env,text=True).strip()

def snapshot_publico():
    return {tabla:sql(f'SELECT row_to_json(r) FROM (SELECT * FROM public.{tabla} ORDER BY id) r')
            for tabla in ['cursos','estudiantes','matriculas','usuarios']}

def request(method,path,token=None,body=None,expected=None):
    headers={'Accept':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    data=json.dumps(body).encode() if body is not None else None
    if body is not None:headers['Content-Type']='application/json'
    req=urllib.request.Request(BASE+path,method=method,data=data,headers=headers)
    try:response=urllib.request.urlopen(req,timeout=25)
    except urllib.error.HTTPError as error:response=error
    raw=response.read().decode()
    result=json.loads(raw) if raw else None
    if expected is not None:assert response.status==expected,(method,path,response.status)
    return response.status,result

def esperar(proceso):
    for _ in range(160):
        if proceso.poll() is not None:raise RuntimeError('Falló el arranque; revisar log')
        try:
            status,data=request('POST','/auth/login',body={'username':'admin','password':variables['LAB_ADMIN_PASSWORD']})
            if status==200:return
        except (urllib.error.URLError,TimeoutError):pass
        time.sleep(.5)
    raise RuntimeError('Tiempo de arranque agotado')

def bloquear_curso(schema,course_id):
    assert re.fullmatch(r'lab2_reto_[a-f0-9]+',schema)
    holder=subprocess.Popen(PG,env=pg_env,stdin=subprocess.PIPE,stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,text=True,bufsize=1)
    holder.stdin.write(f'BEGIN;\nSELECT id FROM {schema}.cursos WHERE id={int(course_id)} FOR UPDATE;\n'
                       '\\echo BLOQUEO_LISTO\nSELECT pg_sleep(5);\nCOMMIT;\n')
    holder.stdin.close()
    for line in holder.stdout:
        if line.strip()=='BLOQUEO_LISTO':return holder
    raise RuntimeError('No se pudo preparar el bloqueo de coordinación')

def carrera(course_id,students,token,schema=None,app_name=None,controlar=False):
    holder=bloquear_curso(schema,course_id) if controlar else None
    barrera=threading.Barrier(3)
    def enviar(student):
        barrera.wait(timeout=10)
        inicio=time.perf_counter_ns()
        status,body=request('POST','/matriculas',token,{'estudianteId':student,'cursoId':course_id})
        fin=time.perf_counter_ns()
        return {'estudianteId':student,'status':status,'respuesta':body,
                'inicioNs':inicio,'finNs':fin,'duracionMs':round((fin-inicio)/1e6,2)}
    esperando=0
    try:
        with ThreadPoolExecutor(max_workers=2) as pool:
            futures=[pool.submit(enviar,student) for student in students]
            barrera.wait(timeout=10)
            if controlar:
                limite=time.monotonic()+4
                while time.monotonic()<limite:
                    esperando=int(sql("SELECT count(*) FROM pg_stat_activity WHERE application_name="
                                      f"'{app_name}' AND wait_event_type='Lock'"))
                    if esperando>=2:break
                    time.sleep(.05)
            respuestas=[future.result(timeout=25) for future in futures]
        assert sorted(r['status'] for r in respuestas)==[201,409],respuestas
        conflicto=next(r for r in respuestas if r['status']==409)
        assert conflicto['respuesta']['detail']=='El curso no tiene cupos disponibles'
        solapadas=max(r['inicioNs'] for r in respuestas)<min(r['finNs'] for r in respuestas)
        assert solapadas,'Las llamadas no se solaparon en el tiempo'
        if controlar:assert esperando>=2,'No se observaron dos sesiones esperando por el bloqueo'
        _,rows=request('GET',f'/cursos/{course_id}/matriculas',token,expected=200)
        activas=[m for m in rows if m['estado']=='ACTIVA']
        assert len(activas)==1,rows
        ganador=next(r['estudianteId'] for r in respuestas if r['status']==201)
        assert activas[0]['estudianteId']==ganador
        if schema:
            assert int(sql(f"SELECT count(*) FROM {schema}.matriculas WHERE curso_id={course_id} AND estado='ACTIVA'"))==1
        return {'cursoId':course_id,'respuestas':respuestas,'intervalosSolapados':solapadas,
                'matriculasActivasFinales':1,'sesionesEsperandoLockObservadas':esperando if controlar else None,
                'bloqueoDeCoordinacion':controlar}
    finally:
        if holder:
            try:holder.wait(timeout=10)
            except subprocess.TimeoutExpired:holder.terminate();holder.wait(timeout=5)

def probar_perfil(profile):
    schema='lab2_reto_'+uuid.uuid4().hex[:12] if profile=='postgres' else None
    app_name='lab2-reto-http-'+uuid.uuid4().hex[:10]
    if schema:sql('CREATE SCHEMA '+schema)
    env=dict(os.environ,**variables,SPRING_PROFILES_ACTIVE=profile,APP_DEMO_ENABLED='true')
    args=['java','-jar',str(ROOT/'target/academia-api-0.0.1-SNAPSHOT.jar'),'--server.port=18080']
    if schema:
        env['DB_URL']=f'jdbc:postgresql://127.0.0.1:5432/academia_db?currentSchema={schema}&ApplicationName={app_name}'
        args+=['--spring.jpa.hibernate.ddl-auto=create-drop',f'--spring.jpa.properties.hibernate.default_schema={schema}']
    report={'perfil':profile,'esquemaTemporal':schema,'casos':{},'rondasConcurrencia':[]}
    proceso=None
    try:
        with (OUT/('servidor-'+profile+'.txt')).open('w') as log:
            proceso=subprocess.Popen(args,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
            esperar(proceso)
            tokens={}
            for role,key in [('admin','LAB_ADMIN_PASSWORD'),('docente','LAB_DOCENTE_PASSWORD'),('estudiante','LAB_ESTUDIANTE_PASSWORD')]:
                _,data=request('POST','/auth/login',body={'username':role,'password':variables[key]},expected=200)
                tokens[role]=data['accessToken']
            _,page=request('GET','/estudiantes',tokens['admin'],expected=200)
            first=next(e for e in page['contenido'] if e['codigo']=='N00123456')
            second=next(e for e in page['contenido'] if e['codigo']=='N00987654')
            status,own=request('GET',f"/estudiantes/{first['id']}/matriculas",tokens['estudiante'],expected=200)
            assert len(own)==2 and all(m['estudianteId']==first['id'] for m in own)
            report['casos']['R1']={'http':status,'matriculasPropias':len(own)}
            status,_=request('GET',f"/estudiantes/{second['id']}/matriculas",tokens['estudiante'],expected=403)
            report['casos']['R2']={'http':status}
            status,_=request('GET',f"/estudiantes/{second['id']}/matriculas",tokens['docente'],expected=200)
            report['casos']['R3']={'http':status}
            status,page=request('GET','/cursos?size=100000',tokens['estudiante'],expected=200)
            assert page['tamanio']==50
            report['casos']['R4']={'http':status,'tamanio':page['tamanio']}
            for numero in range(1,4):
                _,course=request('POST','/cursos',tokens['admin'],
                    {'codigo':f'CON-{numero:03d}','nombre':'Prueba de último cupo',
                     'creditos':4,'docente':'Prueba local','cupos':1},expected=201)
                resultado=carrera(course['id'],[first['id'],second['id']],tokens['docente'],
                                   schema,app_name,controlar=bool(schema and numero==1))
                report['rondasConcurrencia'].append(resultado)
            report['casos']['R5']={'rondas':3,'resultadoEnCadaRonda':[201,409],
                                  'matriculasActivasPorCurso':1}
            if schema:
                report['restriccionesVerificadasEnSchema']=schema
                report['matriculasActivasPruebaSQL']=int(sql(f"SELECT count(*) FROM {schema}.matriculas m JOIN {schema}.cursos c ON c.id=m.curso_id WHERE c.codigo LIKE 'CON-%' AND m.estado='ACTIVA'"))
                assert report['matriculasActivasPruebaSQL']==3
    finally:
        if proceso:
            proceso.terminate();proceso.wait(timeout=30)
        if schema:
            assert re.fullmatch(r'lab2_reto_[a-f0-9]+',schema)
            sql('DROP SCHEMA '+schema+' CASCADE')
            report['esquemaTemporalEliminado']=True
    (OUT/('resultados-'+profile+'.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(profile+': cinco casos aprobados; tres carreras con 201/409 y una sola matrícula activa.')
    return report

antes=snapshot_publico()
reports=[probar_perfil('h2'),probar_perfil('postgres')]
despues=snapshot_publico()
assert antes==despues,'Cambió el contenido de las tablas públicas'
summary={'casosGuia':5,'perfiles':['h2','postgres'],'rondasPorPerfil':3,'rondasTotales':6,
         'solicitudesConcurrentes':12,'todasLasCarrerasCon201Y409':True,
         'unaMatriculaActivaPorCurso':True,'publicoPostgresConservado':True,
         'postgresDosSolicitudesEsperandoLockConfirmadas':True,
         'esquemaTemporalEliminado':True,'sinCambiosEnCodigoFuncional':True}
(OUT/'resumen.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
secret_values=[variables[k] for k in ['JWT_SECRET','DB_PASSWORD','LAB_ADMIN_PASSWORD','LAB_DOCENTE_PASSWORD','LAB_ESTUDIANTE_PASSWORD']]
for f in OUT.iterdir():
    if f.is_file():
        contenido=f.read_text();assert all(secret not in contenido for secret in secret_values)
        assert not re.search(r'eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+',contenido)
print('OK: reto completo en ambos perfiles, tablas públicas conservadas y esquema temporal eliminado.')
