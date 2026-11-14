package pe.edu.upn.academia.dto;

import pe.edu.upn.academia.model.Estudiante;

public record EstudianteResponse(Long id, String codigo, String nombres, String apellidos, String email) {
    public static EstudianteResponse desde(Estudiante estudiante) {
        return new EstudianteResponse(estudiante.getId(), estudiante.getCodigo(), estudiante.getNombres(),
                estudiante.getApellidos(), estudiante.getEmail());
    }
}
