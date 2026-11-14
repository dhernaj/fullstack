package pe.edu.upn.academia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Datos de entrada para modificar únicamente los cupos. */
public record CuposRequest(
        @Min(value = 1, message = "Mínimo 1 cupo")
        @Max(value = 60, message = "Máximo 60 cupos")
        int cupos
) {
}
