package pe.edu.upn.academia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que el cliente puede enviar al crear o actualizar un curso. */
public record CursoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[A-Z]{3}-\\d{3}$", message = "Formato de código: AAA-999")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres")
        String nombre,

        @Min(value = 1, message = "Mínimo 1 crédito")
        @Max(value = 6, message = "Máximo 6 créditos")
        int creditos,

        @NotBlank(message = "El docente es obligatorio")
        @Size(max = 80, message = "El docente admite máximo 80 caracteres")
        String docente,

        @Min(value = 1, message = "Mínimo 1 cupo")
        @Max(value = 60, message = "Máximo 60 cupos")
        int cupos
) {
}
