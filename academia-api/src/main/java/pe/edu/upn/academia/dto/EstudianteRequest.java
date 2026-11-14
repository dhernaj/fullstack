package pe.edu.upn.academia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstudianteRequest(
        @NotBlank(message = "El código es obligatorio") @Size(max = 10) String codigo,
        @NotBlank(message = "Los nombres son obligatorios") @Size(max = 60) String nombres,
        @NotBlank(message = "Los apellidos son obligatorios") @Size(max = 60) String apellidos,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "El correo no es válido")
        @Size(max = 120) String email
) {
}
