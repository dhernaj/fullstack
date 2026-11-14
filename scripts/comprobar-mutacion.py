"""Cambia temporalmente el límite de cupos y exige que su prueba detecte el fallo."""
import hashlib
import json
import os
from pathlib import Path
import subprocess

raiz = Path(__file__).resolve().parents[1]
servicio = raiz / 'academia-api/src/main/java/pe/edu/upn/academia/service/MatriculaService.java'
original = servicio.read_bytes()
regla = b'if (ocupados >= curso.getCupos()) {'
if original.count(regla) != 1:
    raise RuntimeError('La regla cambió; no se modifica el servicio automáticamente.')
carpeta = raiz / 'evidencias/parte-b'
carpeta.mkdir(parents=True, exist_ok=True)
entorno = {k: v for k, v in os.environ.items()
           if k not in {'JWT_SECRET', 'DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'SPRING_PROFILES_ACTIVE', 'APP_DEMO_ENABLED'}
           and not k.startswith('LAB_')}
try:
    servicio.write_bytes(original.replace(regla, b'if (ocupados > curso.getCupos()) {'))
    with (carpeta / 'mutacion-cupos.txt').open('w') as salida:
        ejecucion = subprocess.run([
            './mvnw', '-B', '-Dtest=MatriculaServiceTest#rechazaCursoSinCupos', 'test',
        ], cwd=raiz / 'academia-api', env=entorno, stdout=salida, stderr=subprocess.STDOUT, timeout=240)
finally:
    servicio.write_bytes(original)

texto = (carpeta / 'mutacion-cupos.txt').read_text()
detectada = ejecucion.returncode != 0 and 'Failures: 1' in texto and 'rechazaCursoSinCupos' in texto
restaurado = servicio.read_bytes() == original
(carpeta / 'mutacion-resultado.json').write_text(json.dumps({
    'cambioTemporal': 'ocupados >= cupos por ocupados > cupos',
    'prueba': 'MatriculaServiceTest#rechazaCursoSinCupos',
    'codigoSalida': ejecucion.returncode,
    'mutacionDetectada': detectada,
    'fuenteRestaurada': restaurado,
    'sha256OriginalYRestaurado': hashlib.sha256(original).hexdigest(),
}, ensure_ascii=False, indent=2) + '\n')
if not detectada or not restaurado:
    raise RuntimeError('No se pudo acreditar la prueba de mutación; revisa la evidencia.')
print('La prueba detectó el límite de cupos alterado y el servicio quedó restaurado.')
