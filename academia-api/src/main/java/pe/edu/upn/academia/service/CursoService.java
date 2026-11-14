package pe.edu.upn.academia.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import pe.edu.upn.academia.dto.PaginaResponse;
import pe.edu.upn.academia.dto.CursoResumen;
import pe.edu.upn.academia.repository.MatriculaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upn.academia.dto.CursoRequest;
import pe.edu.upn.academia.dto.CursoResponse;
import pe.edu.upn.academia.exception.ConflictoException;
import pe.edu.upn.academia.exception.RecursoNoEncontradoException;
import pe.edu.upn.academia.model.Curso;
import pe.edu.upn.academia.repository.CursoRepository;

/** Reglas de negocio y transacciones para administrar los cursos. */
@Service
@Transactional(readOnly = true)
public class CursoService {

    private final CursoRepository repository;
    private final MatriculaRepository matriculas;
    private final int maximoPagina;

    public CursoService(CursoRepository repository, MatriculaRepository matriculas,
                        @Value("${spring.data.web.pageable.max-page-size:50}") int maximoPagina) {
        this.repository = repository;
        this.matriculas = matriculas;
        this.maximoPagina = maximoPagina;
    }

    public PaginaResponse<CursoResponse> listar(String docente, Integer creditosMin, Pageable pageable) {
        Pageable limitado = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), maximoPagina), pageable.getSort());
        var pagina = repository.findByActivoTrueAndDocenteContainingIgnoreCaseAndCreditosGreaterThanEqual(
                docente == null ? "" : docente, creditosMin == null ? 0 : creditosMin, limitado);
        return PaginaResponse.desde(pagina.map(CursoResponse::desde));
    }

    public List<CursoResumen> resumenOcupacion() {
        return repository.resumenOcupacion();
    }

    public CursoResponse obtener(Long id) {
        return CursoResponse.desde(buscar(id));
    }

    @Transactional
    public CursoResponse crear(CursoRequest request) {
        validarCodigoUnico(request.codigo(), null);
        Curso curso = new Curso(request.codigo(), request.nombre().trim(), request.creditos(),
                request.docente().trim(), request.cupos());
        return CursoResponse.desde(repository.save(curso));
    }

    @Transactional
    public CursoResponse actualizar(Long id, CursoRequest request) {
        Curso curso = buscar(id);
        validarCodigoUnico(request.codigo(), id);
        curso.setCodigo(request.codigo());
        curso.setNombre(request.nombre().trim());
        curso.setCreditos(request.creditos());
        curso.setDocente(request.docente().trim());
        curso.setCupos(request.cupos());
        return CursoResponse.desde(repository.save(curso));
    }

    @Transactional
    public CursoResponse actualizarCupos(Long id, int cupos) {
        Curso curso = buscar(id);
        curso.setCupos(cupos);
        return CursoResponse.desde(repository.save(curso));
    }

    @Transactional
    public void eliminar(Long id) {
        Curso curso = buscar(id);
        if (matriculas.existsByCursoId(id)) {
            throw new ConflictoException("No se puede eliminar un curso con matrículas");
        }
        curso.setActivo(false);
        repository.save(curso);
    }

    private void validarCodigoUnico(String codigo, Long idActual) {
        repository.findByCodigoIgnoreCase(codigo)
                .filter(otro -> !otro.getId().equals(idActual))
                .ifPresent(otro -> {
                    throw new ConflictoException("Ya existe un curso con código " + codigo);
                });
    }

    private Curso buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Curso " + id + " no encontrado"));
    }
}
