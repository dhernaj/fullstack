\d public.cursos
SELECT count(*) AS cursos_cargados FROM public.cursos;
SELECT column_name, data_type, character_maximum_length, is_nullable, is_identity, identity_generation
FROM information_schema.columns WHERE table_schema='public' AND table_name='cursos' ORDER BY ordinal_position;
SELECT conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint WHERE conrelid='public.cursos'::regclass ORDER BY contype;
