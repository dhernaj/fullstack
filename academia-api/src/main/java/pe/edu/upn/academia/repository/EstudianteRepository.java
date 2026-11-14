package pe.edu.upn.academia.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upn.academia.model.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByCodigoIgnoreCase(String codigo);
    Optional<Estudiante> findByEmailIgnoreCase(String email);
}
