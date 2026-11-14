package pe.edu.upn.academia.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import pe.edu.upn.academia.model.*;
import pe.edu.upn.academia_api.AcademiaApiApplication;

@DataJpaTest
@ContextConfiguration(classes = AcademiaApiApplication.class)
class CursoRepositoryTest {
    @Autowired CursoRepository cursos;
    @Autowired EstudianteRepository estudiantes;
    @Autowired MatriculaRepository matriculas;

    @Test
    void buscaPorDocenteSinDistinguirMayusculas() {
        cursos.save(new Curso("SIS-101", "Programación", 4, "Ana Torres", 40));
        cursos.save(new Curso("SIS-205", "Base de Datos", 4, "Luis Pérez", 35));
        var pagina = cursos.findByDocenteContainingIgnoreCase("ana", PageRequest.of(0, 10));
        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getCodigo()).isEqualTo("SIS-101");
    }

    @Test
    void resumenCuentaSoloLasMatriculasActivas() {
        Curso web = cursos.save(new Curso("SIS-310", "Web", 5, "Carla Ramos", 10));
        Estudiante ana = estudiantes.save(new Estudiante("N00000001", "Ana", "Uno", "a@example.test"));
        Estudiante beto = estudiantes.save(new Estudiante("N00000002", "Beto", "Dos", "b@example.test"));
        matriculas.save(new Matricula(ana, web, LocalDate.now()));
        Matricula retirada = matriculas.save(new Matricula(beto, web, LocalDate.now()));
        retirada.retirar();
        matriculas.flush();
        assertThat(cursos.resumenOcupacion()).singleElement().satisfies(resumen -> {
            assertThat(resumen.matriculados()).isEqualTo(1);
            // El DTO actual conserva cupos y matriculados; no tiene un campo disponibles.
            assertThat(resumen.cupos() - resumen.matriculados()).isEqualTo(9);
        });
    }
}
