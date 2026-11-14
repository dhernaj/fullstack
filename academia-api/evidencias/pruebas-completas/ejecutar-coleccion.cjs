const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const newman = require('newman');
const root = path.resolve(__dirname, '../..');
const out = path.join(root, 'evidencias/pruebas-completas');
const env = {...process.env};
for (const line of fs.readFileSync(path.join(root, '.env.local'), 'utf8').split('\n')) {
  if (line.startsWith('export ')) {
    const pos = line.indexOf('='); env[line.slice(7,pos)] = line.slice(pos+1);
  }
}
function snapshot() {
  const raw = execFileSync('/usr/local/opt/postgresql@17/bin/psql',
    ['-h','127.0.0.1','-U',env.DB_USERNAME,'-d','academia_db','-At','-v','ON_ERROR_STOP=1',
     '-c','SELECT row_to_json(c) FROM (SELECT * FROM cursos ORDER BY id) c'],
    {env:{...env,PGPASSWORD:env.DB_PASSWORD,PGCLIENTENCODING:'UTF8'},encoding:'utf8'});
  return raw.trim().split('\n').filter(Boolean).map(JSON.parse);
}
const initial = snapshot();
const collection = JSON.parse(fs.readFileSync(path.join(root,'postman/academia-guia2-pruebas.postman_collection.json')));
const names = ['Principal','Credito1','Credito6','Cupos60','Cupos1','Nombre100','Docente80'];
const used = new Set(initial.map(c=>c.codigo));
const codes = {};
let index=1;
for (const name of names) {
  let code;
  do { if(index>999) throw new Error('No hay códigos de prueba disponibles'); code='PRB-'+String(index++).padStart(3,'0'); } while(used.has(code));
  used.add(code); codes['codigo'+name]=code;
}
for (const v of collection.variable) if(codes[v.key]) v.value=codes[v.key];
fs.writeFileSync(path.join(out,'variables-ejecucion.json'),JSON.stringify(codes,null,2)+'\n');
const checkpoints=[];
const dbFailures=[];
const created=[];
newman.run({collection,reporters:['cli','json','junit'],
  reporter:{json:{export:path.join(out,'resultado-newman.json')},junit:{export:path.join(out,'resultado-newman.xml')}},
  timeoutRequest:10000, timeoutScript:10000
}, (err,summary)=>{
  const final=snapshot();
  const originalAfter=final.filter(c=>initial.some(old=>old.id===c.id));
  const added=final.filter(c=>!initial.some(old=>old.id===c.id));
  if(JSON.stringify(originalAfter)!==JSON.stringify(initial)) dbFailures.push('Los datos originales cambiaron');
  if(added.length!==7 || added.some(c=>c.activo)) dbFailures.push('Se esperaban siete pruebas nuevas, todas inactivas');
  const report={newmanVersion:require('newman/package.json').version,
    casosGuia:14,peticiones:summary?.run.stats.requests.total,aserciones:summary?.run.stats.assertions.total,
    fallosNewman:summary?.run.failures.length,fallosBase:dbFailures,
    originalesConservados:JSON.stringify(originalAfter)===JSON.stringify(initial),
    cursosPrueba:added,creados:created};
  fs.writeFileSync(path.join(out,'resumen.json'),JSON.stringify(report,null,2)+'\n');
  fs.writeFileSync(path.join(out,'verificaciones-postgresql.json'),JSON.stringify(checkpoints,null,2)+'\n');
  if(err || summary.run.failures.length || dbFailures.length) { console.error('Verificación incompleta:',err?.message,dbFailures); process.exitCode=1; }
  else console.log('OK: colección aprobada, originales conservados y siete cursos de prueba inactivos.');
}).on('request',(err,args)=>{
  if(err) {dbFailures.push(err.message);return;}
  const rows=snapshot();
  const method=args.request.method;
  const code=args.response.code;
  if((method==='POST'&&code===201)||(method==='PUT'&&code===200)) {
    const body=JSON.parse(args.response.stream.toString());
    const row=rows.find(r=>r.id===body.id);
    if(!row || Object.keys(body).some(key=>row[key]!==body[key]))dbFailures.push('Escritura no coincide: '+args.item.name);
    if(method==='POST')created.push({caso:args.item.name,id:body.id,codigo:body.codigo});
  }
  if(method==='DELETE'&&code===204) {
    const match=args.request.url.toString().match(/cursos\/(\d+)/);
    if(!match || rows.find(c=>c.id===Number(match[1]))?.activo!==false)dbFailures.push('DELETE no conservó inactivo: '+args.item.name);
  }
  checkpoints.push({peticion:args.item.name,metodo:method,http:code,cursos:rows});
});
