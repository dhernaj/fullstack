package pe.edu.upn.academia.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upn.academia.dto.CursoRequest;
import pe.edu.upn.academia.exception.ConflictoException;
import pe.edu.upn.academia.exception.RecursoNoEncontradoException;
import pe.edu.upn.academia.model.Curso;
import pe.edu.upn.academia.repository.CursoRepository;
import pe.edu.upn.academia.repository.MatriculaRepository;

@ExtendWith(MockitoExtension.class)
class CursoServiceTest {
    @Mock CursoRepository cursos;
    @Mock MatriculaRepository matriculas;
    private final CursoRequest request = new CursoRequest("SIS-500", "  Pruebas de Software ", 4, " Ana Torres ", 30);

    private CursoService service() { return new CursoService(cursos, matriculas, 50); }
    private Curso conId(Curso curso, long id) {
        ReflectionTestUtils.setField(curso, "id", id);
        return curso;
    }

    @Test
    void crearRecortaEspaciosYGuardaElCurso() {
        when(cursos.findByCodigoIgnoreCase("SIS-500")).thenReturn(Optional.empty());
        when(cursos.save(any(Curso.class))).thenAnswer(inv -> conId(inv.getArgument(0), 10));
        var creado = service().crear(request);
        assertThat(creado.id()).isEqualTo(10);
        assertThat(creado.nombre()).isEqualTo("Pruebas de Software");
        assertThat(creado.docente()).isEqualTo("Ana Torres");
        assertThat(creado.activo()).isTrue();
    }

    @Test
    void crearRechazaElCodigoDuplicadoSinGuardar() {
        when(cursos.findByCodigoIgnoreCase("SIS-500"))
                .thenReturn(Optional.of(conId(new Curso("SIS-500", "Otro", 3, "Docente", 10), 1)));
        assertThatThrownBy(() -> service().crear(request)).isInstanceOf(ConflictoException.class).hasMessageContaining("SIS-500");
        verify(cursos, never()).save(any());
    }

    @Test
    void obtenerInformaQueElCursoNoExiste() {
        when(cursos.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void actualizarPermiteElPropioCodigoYConservaElId() {
        Curso actual = conId(new Curso("SIS-500", "Viejo", 3, "Luis Pérez", 20), 5);
        when(cursos.findById(5L)).thenReturn(Optional.of(actual));
        when(cursos.findByCodigoIgnoreCase("SIS-500")).thenReturn(Optional.of(actual));
        when(cursos.save(actual)).thenReturn(actual);
        var respuesta = service().actualizar(5L, request);
        assertThat(respuesta.id()).isEqualTo(5);
        assertThat(respuesta.nombre()).isEqualTo("Pruebas de Software");
        assertThat(actual.getCupos()).isEqualTo(30);
    }
}
