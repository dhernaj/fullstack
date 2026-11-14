package pe.edu.upn.academia.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upn.academia.repository.UsuarioRepository;

/**
 * Comprueba la propiedad del registro desde @PreAuthorize:
 *   @accesoEstudiante.esPropio(authentication, #id)
 */
@Component("accesoEstudiante")
public class AccesoEstudiante {

    private final UsuarioRepository usuarios;

    public AccesoEstudiante(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public boolean esPropio(Authentication auth, Long estudianteId) {
        if (auth == null || estudianteId == null) {
            return false;
        }
        return usuarios.findByUsername(auth.getName())
                .map(u -> u.getEstudiante() != null && estudianteId.equals(u.getEstudiante().getId()))
                .orElse(false);
    }
}
