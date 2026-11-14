const fs=require('fs');
const path=require('path');
const newman=require('newman');
const root=path.resolve(__dirname,'../../..');
const env={...process.env};
for(const line of fs.readFileSync(path.join(root,'.env.local'),'utf8').split('\n')) {
  if(line.startsWith('export ')){const i=line.indexOf('=');env[line.slice(7,i)]=line.slice(i+1);}
}
const collection=JSON.parse(fs.readFileSync(path.join(root,'postman/academia-lab2-seguridad.postman_collection.json')));
const values={baseUrl:'http://127.0.0.1:18080/api/v1',adminPassword:env.LAB_ADMIN_PASSWORD,
  docentePassword:env.LAB_DOCENTE_PASSWORD,estudiantePassword:env.LAB_ESTUDIANTE_PASSWORD};
for(const variable of collection.variable)variable.value=values[variable.key]??variable.value;
const privateReport=path.resolve(root,'../../work/lab2-seguridad-privado.json');
const secrets=[env.JWT_SECRET,env.DB_PASSWORD,env.LAB_ADMIN_PASSWORD,env.LAB_DOCENTE_PASSWORD,env.LAB_ESTUDIANTE_PASSWORD].filter(Boolean);
function cleanText(text){for(const secret of secrets)text=text.split(secret).join('[omitido]');return text.replace(/eyJ[A-Za-z0-9_-]*\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g,'[token omitido]');}
function clean(value){
  if(typeof value==='string')return cleanText(value);
  if(Array.isArray(value))return value.map(clean);
  if(value&&typeof value==='object'){
    if(value.type==='Buffer'&&Array.isArray(value.data))return {type:'Buffer',data:Array.from(Buffer.from(cleanText(Buffer.from(value.data).toString('utf8'))))};
    return Object.fromEntries(Object.entries(value).map(([key,val])=>[key,clean(val)]));
  }
  return value;
}
newman.run({collection,reporters:['cli','json'],reporter:{json:{export:privateReport}},timeoutRequest:10000,timeoutScript:10000},
  (err,summary)=>{
    let tries=0;
    const finish=()=>{
      if(!fs.existsSync(privateReport)&&tries++<40){setTimeout(finish,50);return;}
      try{
        const report=clean(JSON.parse(fs.readFileSync(privateReport,'utf8')));
        fs.writeFileSync(path.join(__dirname,'resultado-newman.json'),JSON.stringify(report,null,2)+'\n');
        const results=summary.run.executions.map(e=>({caso:e.item.name,status:e.response?.code,
          aserciones:e.assertions?.map(a=>({nombre:a.assertion,aprobada:!a.error}))??[]}));
        fs.writeFileSync(path.join(__dirname,'resumen.json'),JSON.stringify({casosGuia:12,
          peticiones:summary.run.stats.requests.total,aserciones:summary.run.stats.assertions.total,
          fallos:summary.run.failures.length,secretosYTokensOmitidos:true,resultados:results},null,2)+'\n');
        fs.unlinkSync(privateReport);
        if(err||summary.run.failures.length){console.error('La colección contiene fallos.');process.exitCode=1;}
        else console.log('OK: casos por rol, JWT y CORS aprobados; reporte sin contraseñas ni tokens.');
      }catch(error){console.error('No se pudo guardar el reporte depurado:',error.message);process.exitCode=1;}
    };
    finish();
  });
