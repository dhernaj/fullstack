package pe.edu.upn.academia.dto;

import pe.edu.upn.academia.model.Curso;

/** Datos de un curso que la API devuelve al cliente. */
public record CursoResponse(
        Long id,
        String codigo,
        String nombre,
        int creditos,
        String docente,
        int cupos,
        boolean activo
) {
    public static CursoResponse desde(Curso curso) {
        return new CursoResponse(curso.getId(), curso.getCodigo(), curso.getNombre(),
                curso.getCreditos(), curso.getDocente(), curso.getCupos(), curso.isActivo());
    }
}
