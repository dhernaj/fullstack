SELECT current_database() AS base_de_datos, version();
SELECT id, codigo, nombre, creditos, docente, cupos, activo
FROM laboratorio3_angular.cursos
WHERE codigo = 'SIS-500';
