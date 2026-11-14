package pe.edu.upn.academia.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatriculaRequest(
        @NotNull(message = "El estudiante es obligatorio")
        @Positive(message = "El identificador del estudiante debe ser positivo")
        Long estudianteId,
        @NotNull(message = "El curso es obligatorio")
        @Positive(message = "El identificador del curso debe ser positivo")
        Long cursoId
) {
}
