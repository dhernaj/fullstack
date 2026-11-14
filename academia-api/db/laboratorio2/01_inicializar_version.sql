-- Actualización de la base existente del Laboratorio 1 antes de usar @Version.
-- No modifica los campos del catálogo ni reinicia sus identificadores.
BEGIN;
ALTER TABLE cursos ADD COLUMN IF NOT EXISTS version BIGINT;
UPDATE cursos SET version = 0 WHERE version IS NULL;
COMMIT;
