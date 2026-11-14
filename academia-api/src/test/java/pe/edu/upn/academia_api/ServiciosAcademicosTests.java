package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.dto.*;
import pe.edu.upn.academia.exception.*;
import pe.edu.upn.academia.model.*;
import pe.edu.upn.academia.repository.*;
import pe.edu.upn.academia.service.*;

@SpringBootTest
@Transactional
class ServiciosAcademicosTests {
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;
    @Autowired private CursoRepository cursos;
    @Autowired private EstudianteRepository estudiantes;
    @Autowired private MatriculaRepository matriculas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private CursoService cursoService;
    @Autowired private EstudianteService estudianteService;
    @Autowired private MatriculaService matriculaService;

    @BeforeEach
    void aislarDatos() {
        // Todo se revierte al terminar cada prueba, incluidos estos borrados.
        matriculas.deleteAll();
        usuarios.deleteAll();
        estudiantes.deleteAll();
        cursos.deleteAll();
        em.flush();
    }

    private Curso curso(String codigo, int creditos, int cupos) {
        return cursos.saveAndFlush(new Curso(codigo, codigo, creditos, "Ana Torres", cupos));
    }

    private Estudiante estudiante(String codigo) {
        return estudiantes.saveAndFlush(new Estudiante(codigo, "Lucía", codigo, codigo+"@example.test"));
    }

    private MatriculaResponse matricular(Estudiante estudiante, Curso curso) {
        return matriculaService.matricular(new MatriculaRequest(estudiante.getId(), curso.getId()));
    }

    @Test
    void paginaYOrdenaLosCursosDesdeLaBase() {
        curso("PAG-101", 3, 30);
        curso("PAG-102", 6, 30);
        curso("PAG-103", 4, 30);
        var pagina = cursoService.listar(null, null,
                PageRequest.of(0, 1, Sort.by("creditos").descending()));
        assertEquals(3, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
        assertEquals(1, pagina.contenido().size());
        assertEquals(6, pagina.contenido().get(0).creditos());
        assertFalse(pagina.ultima());
    }

    @Test
    void limitaElTamanioDePaginaA50() {
        curso("PAG-104", 4, 30);
        assertEquals(50, cursoService.listar(null, null, PageRequest.of(0, 100000)).tamanio());
        estudiante("N00888001");
        assertEquals(50, estudianteService.listar(PageRequest.of(0, 100000)).tamanio());
    }

    @Test
    void combinaFiltrosYExcluyeInactivos() {
        Curso primero = curso("PAG-105", 4, 30);
        curso("PAG-106", 3, 30);
        Curso inactivo = curso("PAG-107", 6, 30);
        inactivo.setActivo(false);
        em.flush();
        var filtrados = cursoService.listar("ANA", 4, PageRequest.of(0, 10));
        assertEquals(1, filtrados.totalElementos());
        assertEquals(primero.getId(), filtrados.contenido().get(0).id());
        assertEquals(0, cursoService.listar("%", null, PageRequest.of(0, 10)).totalElementos());
    }

    @Test
    void consultaCreditosYProyectaSoloMatriculasActivas() {
        Curso curso = curso("RES-101", 6, 3);
        Curso vacio = curso("RES-102", 3, 3);
        var primera = matricular(estudiante("N00888002"), curso);
        var segunda = matricular(estudiante("N00888003"), curso);
        matriculaService.retirar(segunda.id());
        em.flush();
        var resumen = cursoService.resumenOcupacion();
        assertEquals(1, resumen.stream().filter(r -> r.id().equals(curso.getId())).findFirst().orElseThrow().matriculados());
        assertEquals(0, resumen.stream().filter(r -> r.id().equals(vacio.getId())).findFirst().orElseThrow().matriculados());
        assertEquals(curso.getId(), cursos.buscarActivosConCreditosMinimos(5).get(0).getId());
        assertNotNull(primera.id());
    }

    @Test
    void creaConsultaYPaginaEstudiantes() {
        var creado = estudianteService.crear(new EstudianteRequest(
                " N00888004 ", " Lucía ", " Pérez ", " LUCIA@EXAMPLE.TEST "));
        assertEquals("Lucía", creado.nombres());
        assertEquals("lucia@example.test", creado.email());
        assertEquals(creado, estudianteService.obtener(creado.id()));
        var pagina = estudianteService.listar(PageRequest.of(0, 1, Sort.by("apellidos")));
        assertEquals(1, pagina.totalElementos());
        assertEquals(creado, pagina.contenido().get(0));
    }

    @Test
    void rechazaCodigoOCorreoDeEstudianteRepetido() {
        estudiante("N00888005");
        assertThrows(ConflictoException.class, () -> estudianteService.crear(
                new EstudianteRequest("n00888005", "A", "B", "otro@example.test")));
        assertThrows(ConflictoException.class, () -> estudianteService.crear(
                new EstudianteRequest("N00888006", "A", "B", "N00888005@EXAMPLE.TEST")));
    }

    @Test
    void consultaLaCuentaPorNombreDeUsuario() {
        usuarios.saveAndFlush(new Usuario("cuenta-prueba", "hash-sintetico-no-es-una-cuenta-real", Rol.ADMIN));
        assertEquals(Rol.ADMIN, usuarios.findByUsername("cuenta-prueba").orElseThrow().getRol());
        assertTrue(usuarios.findByUsername("no-existe").isEmpty());
    }

    @Test
    void matriculaYConvierteLosDatosDentroDeLaTransaccion() {
        Curso curso = curso("MAT-101", 4, 2);
        Estudiante estudiante = estudiante("N00888007");
        var respuesta = matricular(estudiante, curso);
        em.flush();
        em.clear();
        assertEquals(EstadoMatricula.ACTIVA, respuesta.estado());
        assertEquals(LocalDate.now(), respuesta.fecha());
        assertEquals("Lucía N00888007", respuesta.estudianteNombre());
        assertEquals(1, matriculaService.listarPorCurso(curso.getId()).size());
        assertEquals(respuesta, matriculaService.listarPorEstudiante(estudiante.getId()).get(0));
    }

    @Test
    void rechazaMatriculaRepetidaInclusoTrasElRetiro() {
        Curso curso = curso("MAT-102", 4, 3);
        Estudiante estudiante = estudiante("N00888008");
        var primera = matricular(estudiante, curso);
        matriculaService.retirar(primera.id());
        assertThrows(ConflictoException.class, () -> matricular(estudiante, curso));
    }

    @Test
    void rechazaCursosInactivos() {
        Curso curso = curso("MAT-103", 4, 3);
        curso.setActivo(false);
        em.flush();
        Estudiante estudiante = estudiante("N00888009");
        assertThrows(ConflictoException.class, () -> matricular(estudiante, curso));
    }

    @Test
    void rechazaUnaMatriculaCuandoNoQuedanCupos() {
        Curso curso = curso("MAT-104", 4, 1);
        matricular(estudiante("N00888010"), curso);
        Estudiante segundo = estudiante("N00888011");
        assertThrows(ConflictoException.class, () -> matricular(segundo, curso));
    }

    @Test
    void informaReferenciasInexistentes() {
        Curso curso = curso("MAT-105", 4, 3);
        Estudiante estudiante = estudiante("N00888012");
        assertThrows(RecursoNoEncontradoException.class,
                () -> matriculaService.matricular(new MatriculaRequest(Long.MAX_VALUE, curso.getId())));
        assertThrows(RecursoNoEncontradoException.class,
                () -> matriculaService.matricular(new MatriculaRequest(estudiante.getId(), Long.MAX_VALUE)));
        assertThrows(RecursoNoEncontradoException.class, () -> matriculaService.retirar(Long.MAX_VALUE));
        assertThrows(RecursoNoEncontradoException.class, () -> matriculaService.listarPorCurso(Long.MAX_VALUE));
        assertThrows(RecursoNoEncontradoException.class, () -> matriculaService.listarPorEstudiante(Long.MAX_VALUE));
        assertThrows(RecursoNoEncontradoException.class, () -> estudianteService.obtener(Long.MAX_VALUE));
    }

    @Test
    void retirarLiberaLaPlazaSinBorrarLaMatricula() {
        Curso curso = curso("MAT-106", 4, 1);
        var primera = matricular(estudiante("N00888013"), curso);
        assertEquals(EstadoMatricula.RETIRADA, matriculaService.retirar(primera.id()).estado());
        var segunda = matricular(estudiante("N00888014"), curso);
        em.flush();
        assertEquals(EstadoMatricula.ACTIVA, segunda.estado());
        assertEquals(2, matriculas.count());
        assertEquals(1, matriculas.countByCursoIdAndEstado(curso.getId(), EstadoMatricula.ACTIVA));
    }

    @Test
    void impideEliminarCursosConMatriculas() {
        Curso curso = curso("MAT-107", 4, 3);
        matricular(estudiante("N00888015"), curso);
        assertThrows(ConflictoException.class, () -> cursoService.eliminar(curso.getId()));
    }

    @Test
    void joinFetchReduceLasConsultasAlConstruirLosDtos() {
        Curso curso = curso("MAT-108", 4, 3);
        matricular(estudiante("N00888016"), curso);
        matricular(estudiante("N00888017"), curso);
        em.flush();
        em.clear();
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        boolean anterior = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            assertTrue(cursos.existsById(curso.getId()));
            var ingenua = matriculas.findByCursoId(curso.getId()).stream().map(MatriculaResponse::desde).toList();
            long consultasIngenuas = stats.getPrepareStatementCount();
            em.clear();
            stats.clear();
            var optimizada = matriculaService.listarPorCurso(curso.getId());
            long consultasOptimizadas = stats.getPrepareStatementCount();
            assertEquals(2, ingenua.size());
            assertEquals(2, optimizada.size());
            assertEquals(5, consultasIngenuas);
            assertEquals(2, consultasOptimizadas);
            System.out.println("CONSULTAS_MATRICULAS: sin_fetch="+consultasIngenuas+", con_fetch="+consultasOptimizadas);
        } finally {
            stats.setStatisticsEnabled(anterior);
        }
    }
}
