"""Comprueba que la cobertura existente rechaza 95 % y admite el mínimo de la guía."""
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

raiz = Path(__file__).resolve().parents[1]
api = raiz / 'academia-api'
carpeta = raiz / 'evidencias/parte-b'
reporte = api / 'target/site/jacoco/jacoco.xml'
if not reporte.exists():
    raise RuntimeError('Ejecuta primero ./mvnw clean verify dentro de academia-api.')
resultados = {}
for minimo, nombre in [('0.95', 'umbral-95.txt'), ('0.50', 'umbral-50.txt')]:
    with (carpeta / nombre).open('w') as salida:
        ejecucion = subprocess.run([
            './mvnw', '-B', 'jacoco:check@check', f'-Djacoco.minimum.line.coverage={minimo}',
        ], cwd=api, stdout=salida, stderr=subprocess.STDOUT, timeout=180)
    texto = (carpeta / nombre).read_text()
    if minimo == '0.95':
        correcto = ejecucion.returncode != 0 and 'lines covered ratio' in texto and '0.95' in texto
    else:
        correcto = ejecucion.returncode == 0 and 'All coverage checks have been met.' in texto
    resultados[minimo] = {'codigoSalida': ejecucion.returncode, 'resultadoEsperado': correcto}
    if not correcto:
        raise RuntimeError('El resultado no fue el esperado para el mínimo ' + minimo)

arbol = ET.parse(reporte).getroot()
lineas = arbol.find("counter[@type='LINE']")
cubiertas, omitidas = int(lineas.get('covered')), int(lineas.get('missed'))
resultado = {'lineasCubiertas': cubiertas, 'lineasSinCubrir': omitidas,
             'coberturaLineasPorcentaje': round(100 * cubiertas / (cubiertas + omitidas), 2),
             'verificaciones': resultados, 'minimoConfiguradoEnPom': '0.50'}
(carpeta / 'cobertura-resumen.json').write_text(json.dumps(resultado, ensure_ascii=False, indent=2) + '\n')
print(f'Cobertura: {resultado["coberturaLineasPorcentaje"]} %. El umbral 95 % fue rechazado y el 50 % fue aprobado.')
