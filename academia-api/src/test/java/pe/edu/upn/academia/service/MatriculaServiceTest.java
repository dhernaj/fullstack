package pe.edu.upn.academia.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upn.academia.dto.MatriculaRequest;
import pe.edu.upn.academia.exception.ConflictoException;
import pe.edu.upn.academia.model.*;
import pe.edu.upn.academia.repository.*;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {
    @Mock MatriculaRepository matriculas;
    @Mock EstudianteRepository estudiantes;
    @Mock CursoRepository cursos;
    @InjectMocks MatriculaService service;
    private Curso curso;
    private final MatriculaRequest request = new MatriculaRequest(1L, 2L);

    @BeforeEach
    void preparar() {
        Estudiante estudiante = new Estudiante("N00100001", "María", "Quispe", "m@example.test");
        ReflectionTestUtils.setField(estudiante, "id", 1L);
        curso = new Curso("SIS-310", "Web", 5, "Carla Ramos", 2);
        ReflectionTestUtils.setField(curso, "id", 2L);
        when(estudiantes.findById(1L)).thenReturn(Optional.of(estudiante));
        when(cursos.findByIdParaMatricula(2L)).thenReturn(Optional.of(curso));
    }

    @Test
    void rechazaMatriculaDuplicada() {
        when(matriculas.existsByEstudianteIdAndCursoId(1L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> service.matricular(request))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("ya está matriculado");
        verify(matriculas, never()).save(any());
    }

    @Test
    void rechazaCursoSinCupos() {
        when(matriculas.countByCursoIdAndEstado(2L, EstadoMatricula.ACTIVA)).thenReturn(2L);
        assertThatThrownBy(() -> service.matricular(request))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("cupos");
        verify(matriculas, never()).save(any());
    }

    @Test
    void rechazaCursoInactivo() {
        curso.setActivo(false);
        assertThatThrownBy(() -> service.matricular(request))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("no está activo");
        verify(matriculas, never()).save(any());
    }

    @Test
    void aceptaElUltimoCupoDisponible() {
        when(matriculas.countByCursoIdAndEstado(2L, EstadoMatricula.ACTIVA)).thenReturn(1L);
        when(matriculas.save(any(Matricula.class))).thenAnswer(inv -> {
            Matricula guardada = inv.getArgument(0);
            ReflectionTestUtils.setField(guardada, "id", 10L);
            return guardada;
        });
        var respuesta = service.matricular(request);
        assertThat(respuesta.id()).isEqualTo(10);
        assertThat(respuesta.estado()).isEqualTo(EstadoMatricula.ACTIVA);
        verify(cursos).findByIdParaMatricula(2L);
    }
}
