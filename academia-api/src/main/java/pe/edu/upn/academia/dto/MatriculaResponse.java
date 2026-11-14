package pe.edu.upn.academia.dto;

import java.time.LocalDate;
import pe.edu.upn.academia.model.EstadoMatricula;
import pe.edu.upn.academia.model.Matricula;

public record MatriculaResponse(Long id, Long estudianteId, String estudianteNombre,
                                Long cursoId, String cursoNombre, LocalDate fecha,
                                EstadoMatricula estado) {
    public static MatriculaResponse desde(Matricula matricula) {
        return new MatriculaResponse(matricula.getId(), matricula.getEstudiante().getId(),
                matricula.getEstudiante().getNombreCompleto(), matricula.getCurso().getId(),
                matricula.getCurso().getNombre(), matricula.getFecha(), matricula.getEstado());
    }
}
