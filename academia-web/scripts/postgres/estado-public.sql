SELECT 'cursos' AS tabla, count(*) AS filas, md5(coalesce(string_agg(row_to_json(t)::text, E'\n' ORDER BY id), '')) AS huella FROM public.cursos t
UNION ALL SELECT 'estudiantes', count(*), md5(coalesce(string_agg(row_to_json(t)::text, E'\n' ORDER BY id), '')) FROM public.estudiantes t
UNION ALL SELECT 'matriculas', count(*), md5(coalesce(string_agg(row_to_json(t)::text, E'\n' ORDER BY id), '')) FROM public.matriculas t
UNION ALL SELECT 'usuarios', count(*), md5(coalesce(string_agg(row_to_json(t)::text, E'\n' ORDER BY id), '')) FROM public.usuarios t;
