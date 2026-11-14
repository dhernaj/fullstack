package pe.edu.upn.academia.service;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.dto.EstudianteRequest;
import pe.edu.upn.academia.dto.EstudianteResponse;
import pe.edu.upn.academia.dto.PaginaResponse;
import pe.edu.upn.academia.exception.ConflictoException;
import pe.edu.upn.academia.exception.RecursoNoEncontradoException;
import pe.edu.upn.academia.model.Estudiante;
import pe.edu.upn.academia.repository.EstudianteRepository;

@Service
@Transactional(readOnly = true)
public class EstudianteService {
    private final EstudianteRepository estudiantes;
    private final int maximoPagina;

    public EstudianteService(EstudianteRepository estudiantes,
                            @Value("${spring.data.web.pageable.max-page-size:50}") int maximoPagina) {
        this.estudiantes = estudiantes;
        this.maximoPagina = maximoPagina;
    }

    public PaginaResponse<EstudianteResponse> listar(Pageable pageable) {
        Pageable limitado = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), maximoPagina), pageable.getSort());
        return PaginaResponse.desde(estudiantes.findAll(limitado).map(EstudianteResponse::desde));
    }

    public EstudianteResponse obtener(Long id) {
        return estudiantes.findById(id).map(EstudianteResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado"));
    }

    @Transactional
    public EstudianteResponse crear(EstudianteRequest request) {
        String codigo = request.codigo().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (estudiantes.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new ConflictoException("Ya existe un estudiante con ese código");
        }
        if (estudiantes.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictoException("Ya existe un estudiante con ese correo");
        }
        Estudiante estudiante = new Estudiante(codigo, request.nombres().trim(),
                request.apellidos().trim(), email);
        return EstudianteResponse.desde(estudiantes.save(estudiante));
    }
}
