package pe.edu.upn.academia.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upn.academia.dto.MatriculaRequest;
import pe.edu.upn.academia.dto.MatriculaResponse;
import pe.edu.upn.academia.exception.ConflictoException;
import pe.edu.upn.academia.exception.RecursoNoEncontradoException;
import pe.edu.upn.academia.model.Curso;
import pe.edu.upn.academia.model.EstadoMatricula;
import pe.edu.upn.academia.model.Estudiante;
import pe.edu.upn.academia.model.Matricula;
import pe.edu.upn.academia.repository.CursoRepository;
import pe.edu.upn.academia.repository.EstudianteRepository;
import pe.edu.upn.academia.repository.MatriculaRepository;

@Service
@Transactional(readOnly = true)
public class MatriculaService {

    private final MatriculaRepository matriculas;
    private final EstudianteRepository estudiantes;
    private final CursoRepository cursos;

    public MatriculaService(MatriculaRepository matriculas, EstudianteRepository estudiantes,
                            CursoRepository cursos) {
        this.matriculas = matriculas;
        this.estudiantes = estudiantes;
        this.cursos = cursos;
    }

    /**
     * Regla de negocio: un estudiante se matricula una sola vez por curso,
     * el curso debe estar activo y tener cupos disponibles.
     */
    @Transactional
    public MatriculaResponse matricular(MatriculaRequest req) {
        Estudiante estudiante = estudiantes.findById(req.estudianteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado"));
        // El bloqueo se conserva hasta que termina la transacción.
        Curso curso = cursos.findByIdParaMatricula(req.cursoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado"));

        if (!curso.isActivo()) {
            throw new ConflictoException("El curso no está activo");
        }
        if (matriculas.existsByEstudianteIdAndCursoId(estudiante.getId(), curso.getId())) {
            throw new ConflictoException("El estudiante ya está matriculado en el curso");
        }
        long ocupados = matriculas.countByCursoIdAndEstado(curso.getId(), EstadoMatricula.ACTIVA);
        if (ocupados >= curso.getCupos()) {
            throw new ConflictoException("El curso no tiene cupos disponibles");
        }
        Matricula m = matriculas.save(new Matricula(estudiante, curso, LocalDate.now()));
        return MatriculaResponse.desde(m);
    }

    public List<MatriculaResponse> listarPorCurso(Long cursoId) {
        if (!cursos.existsById(cursoId)) {
            throw new RecursoNoEncontradoException("Curso no encontrado");
        }
        // Se comprueba la existencia y se cargan las relaciones en una sola consulta adicional.
        return matriculas.findByCursoConDetalle(cursoId).stream()
                .map(MatriculaResponse::desde)
                .toList();
    }

    // Consulta de matrículas por estudiante.
    public List<MatriculaResponse> listarPorEstudiante(Long estudianteId) {
        if (!estudiantes.existsById(estudianteId)) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado");
        }
        return matriculas.findByEstudianteConDetalle(estudianteId).stream()
                .map(MatriculaResponse::desde)
                .toList();
    }

    @Transactional
    public MatriculaResponse retirar(Long id) {
        Matricula m = matriculas.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Matrícula no encontrada"));
        m.retirar();
        return MatriculaResponse.desde(m);
    }
}
