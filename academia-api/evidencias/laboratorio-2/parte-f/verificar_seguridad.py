"""Revisión local de controles de acceso y JWT, sin modificar registros."""
from pathlib import Path
import base64,hmac,hashlib,json,os,re,stat,subprocess,time,urllib.request,urllib.error
ROOT=Path(__file__).resolve().parents[3]
OUT=Path(__file__).resolve().parent
BASE=os.environ.get('LAB_API_URL','http://127.0.0.1:8080/api/v1')
variables=dict(line[7:].split('=',1) for line in (ROOT/'.env.local').read_text().splitlines() if line.startswith('export '))
secrets=[variables[k] for k in ['JWT_SECRET','DB_PASSWORD','LAB_ADMIN_PASSWORD','LAB_DOCENTE_PASSWORD','LAB_ESTUDIANTE_PASSWORD']]
records=[]
def clean(value):
    if isinstance(value,dict):return {k:('[token omitido]' if k=='accessToken' else clean(v)) for k,v in value.items()}
    if isinstance(value,list):return [clean(v) for v in value]
    if isinstance(value,str):
        for secret in secrets:value=value.replace(secret,'[omitido]')
        return re.sub(r'eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*','[token omitido]',value)
    return value

def request(caso,method,path,status,token=None,body=None,headers=None):
    req_headers={'Accept':'application/json'}
    if token:req_headers['Authorization']='Bearer '+token
    if headers:req_headers.update(headers)
    data=json.dumps(body).encode() if body is not None else None
    if body is not None:req_headers['Content-Type']='application/json'
    req=urllib.request.Request(BASE+path,data=data,method=method,headers=req_headers)
    try:response=urllib.request.urlopen(req,timeout=10)
    except urllib.error.HTTPError as error:response=error
    raw=response.read().decode()
    try:result=json.loads(raw)
    except json.JSONDecodeError:result=raw
    assert response.status==status,(caso,response.status,clean(result))
    assert not response.headers.get('Set-Cookie'),caso
    if status in [401,403] and 'application/problem+json' in response.headers.get('Content-Type',''):
        assert not any(t in raw for t in ['org.springframework','java.lang','stackTrace','"trace"'])
    records.append({'caso':caso,'esperado':status,'obtenido':response.status,
                    'resultado':clean(result),'cookieSesion':False})
    return result,response.headers

def sql(query):
    return subprocess.check_output(['/usr/local/opt/postgresql@17/bin/psql','-h','127.0.0.1',
        '-U',variables['DB_USERNAME'],'-d','academia_db','-At','-v','ON_ERROR_STOP=1','-c',query],
        env=dict(os.environ,PGPASSWORD=variables['DB_PASSWORD'],PGCLIENTENCODING='UTF8'),text=True).strip()

snapshot_query='SELECT row_to_json(c) FROM (SELECT * FROM cursos ORDER BY id) c'
antes=sql(snapshot_query)
tokens={}
for role,name,var in [('admin','admin','LAB_ADMIN_PASSWORD'),('docente','docente','LAB_DOCENTE_PASSWORD'),('estudiante','estudiante','LAB_ESTUDIANTE_PASSWORD')]:
    result,_=request('Login '+role,'POST','/auth/login',200,body={'username':name,'password':variables[var]})
    tokens[role]=result['accessToken']
result,headers=request('Sin token','GET','/cursos',401)
assert headers.get('WWW-Authenticate')=='Bearer'
wrong,_=request('Clave incorrecta','POST','/auth/login',401,body={'username':'admin','password':'clave-incorrecta-de-revision'})
unknown,_=request('Usuario desconocido','POST','/auth/login',401,body={'username':'no-existe','password':'clave-incorrecta-de-revision'})
assert wrong['detail']==unknown['detail']=='Usuario o contraseña incorrectos'
page,_=request('Página máxima','GET','/cursos?size=100000',200,tokens['estudiante'])
assert page['tamanio']==50
course=next(c for c in page['contenido'] if c['codigo']=='SIS-101')
request('Estudiante no consulta matrículas de curso','GET',f"/cursos/{course['id']}/matriculas",403,tokens['estudiante'])
request('Docente consulta matrículas de curso','GET',f"/cursos/{course['id']}/matriculas",200,tokens['docente'])
page,_=request('Admin consulta estudiantes','GET','/estudiantes',200,tokens['admin'])
first=next(e for e in page['contenido'] if e['codigo']=='N00123456')
second=next(e for e in page['contenido'] if e['codigo']=='N00987654')
owned,_=request('Matrículas propias','GET',f"/estudiantes/{first['id']}/matriculas",200,tokens['estudiante'])
assert owned and all(m['estudianteId']==first['id'] for m in owned)
request('Matrículas de otra persona','GET',f"/estudiantes/{second['id']}/matriculas",403,tokens['estudiante'])
request('Estudiante no crea cursos','POST','/cursos',403,tokens['estudiante'],body={})

basic=base64.b64encode(('admin:'+variables['LAB_ADMIN_PASSWORD']).encode()).decode()
request('Basic no autentica','GET','/cursos',401,headers={'Authorization':'Basic '+basic})
request('Cookie con token no autentica','GET','/cursos',401,headers={'Cookie':'access_token='+tokens['admin']})

def encode(value):return base64.urlsafe_b64encode(json.dumps(value,separators=(',',':')).encode()).rstrip(b'=').decode()
now=int(time.time())
def signed(issuer,exp):
    head=encode({'alg':'HS256','typ':'JWT'})
    body=encode({'iss':issuer,'sub':'admin','iat':now-600,'exp':exp,'roles':['ADMIN']})
    sig=hmac.new(base64.b64decode(variables['JWT_SECRET']),f'{head}.{body}'.encode(),hashlib.sha256).digest()
    return head+'.'+body+'.'+base64.urlsafe_b64encode(sig).rstrip(b'=').decode()
request('Token vencido','GET','/cursos',401,signed('academia-api',now-300))
request('Emisor incorrecto','GET','/cursos',401,signed('otro-emisor',now+300))
unsigned=encode({'alg':'none','typ':'JWT'})+'.'+encode({'iss':'academia-api','sub':'admin','exp':now+300,'roles':['ADMIN']})+'.'
request('Token sin firma','GET','/cursos',401,unsigned)
parts=tokens['estudiante'].split('.')
claims=json.loads(base64.urlsafe_b64decode(parts[1]+'='*(-len(parts[1])%4)))
claims['roles']=['ADMIN']
request('Rol alterado sin firma válida','GET','/cursos',401,parts[0]+'.'+encode(claims)+'.'+parts[2])
_,headers=request('CORS autorizado','OPTIONS','/cursos',200,headers={'Origin':'http://localhost:4200','Access-Control-Request-Method':'GET'})
assert headers.get('Access-Control-Allow-Origin')=='http://localhost:4200'
request('CORS ajeno','OPTIONS','/cursos',403,headers={'Origin':'http://otro-origen.test','Access-Control-Request-Method':'GET'})
assert sql(snapshot_query)==antes
assert stat.S_IMODE((ROOT/'.env.local').stat().st_mode)==0o600
tracked=subprocess.check_output(['git','ls-files'],cwd=ROOT,text=True).splitlines()
assert '.env.local' not in tracked
for name in tracked:
    p=ROOT/name
    if p.is_file():assert all(secret.encode() not in p.read_bytes() for secret in secrets),name
props=(ROOT/'src/main/resources/application.properties').read_text()
assert 'app.jwt.secret=${JWT_SECRET}' in props and '${JWT_SECRET:' not in props
assert len(base64.b64decode(variables['JWT_SECRET']))>=32
report={'peticiones':len(records),'comprobaciones':records,'sinCookiesDeSesion':True,
        'catalogoPostgresConservado':True,'credencialesFueraDeGit':True,'permisosArchivoLocal':'600',
        'secretoJwtSinValorPorDefecto':True,'minimoBitsClaveJwt':256,
        'pendientes':['Límite de intentos de login','Revocación de tokens',
                      'Prueba de matrículas simultáneas en el reto'],'sinCambiosFuncionales':True}
(OUT/'resultados.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
for secret in secrets:assert secret not in (OUT/'resultados.json').read_text()
print(f'OK: {len(records)} peticiones de revisión aprobadas; sin cambios en los datos.')
