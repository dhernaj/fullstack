package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.model.Curso;
import pe.edu.upn.academia.model.Estudiante;
import pe.edu.upn.academia.model.Matricula;
import pe.edu.upn.academia.model.EstadoMatricula;
import pe.edu.upn.academia.model.Usuario;
import pe.edu.upn.academia.model.Rol;

@SpringBootTest
@Transactional
class ModeloAcademicoTests {
    @Autowired private EntityManager em;
    @Autowired private EntityManagerFactory emf;

    private Estudiante estudiante() {
        Estudiante estudiante = new Estudiante("N00999001", "Lucía", "Pérez", "prueba-modelo@example.test");
        em.persist(estudiante);
        return estudiante;
    }

    private Curso curso() {
        Curso curso = new Curso("MOD-901", "Curso de prueba", 4, "Docente de prueba", 30);
        em.persist(curso);
        return curso;
    }

    private Matricula matricula() {
        Matricula matricula = new Matricula(estudiante(), curso(), LocalDate.of(2026, 11, 11));
        em.persist(matricula);
        em.flush();
        return matricula;
    }

    @Test
    void persisteUsuarioEstudianteMatriculaYEstados() {
        Matricula matricula = matricula();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("clave-de-prueba");
        Usuario usuario = new Usuario("modelo-prueba", hash, Rol.ESTUDIANTE);
        usuario.vincularEstudiante(matricula.getEstudiante());
        em.persist(usuario);
        em.flush();
        Long matriculaId = matricula.getId();
        Long usuarioId = usuario.getId();
        em.clear();

        Usuario almacenado = em.find(Usuario.class, usuarioId);
        assertEquals(Rol.ESTUDIANTE, almacenado.getRol());
        assertEquals("Lucía Pérez", almacenado.getEstudiante().getNombreCompleto());
        assertTrue(encoder.matches("clave-de-prueba", almacenado.getPassword()));
        assertNotEquals("clave-de-prueba", almacenado.getPassword());
        Matricula guardada = em.find(Matricula.class, matriculaId);
        assertEquals(EstadoMatricula.ACTIVA, guardada.getEstado());
        assertEquals(LocalDate.of(2026, 11, 11), guardada.getFecha());
        guardada.retirar();
        em.flush();
        em.clear();
        assertEquals(EstadoMatricula.RETIRADA, em.find(Matricula.class, matriculaId).getEstado());
    }

    @Test
    void lasRelacionesDeMatriculaSeCarganDeFormaPerezosa() {
        Long id = matricula().getId();
        em.clear();
        Matricula guardada = em.find(Matricula.class, id);
        assertFalse(emf.getPersistenceUnitUtil().isLoaded(guardada, "estudiante"));
        assertFalse(emf.getPersistenceUnitUtil().isLoaded(guardada, "curso"));
        assertEquals("N00999001", guardada.getEstudiante().getCodigo());
        assertEquals("MOD-901", guardada.getCurso().getCodigo());
    }

    @Test
    void permiteNavegarLasRelacionesInversas() {
        Matricula matricula = matricula();
        Long cursoId = matricula.getCurso().getId();
        Long estudianteId = matricula.getEstudiante().getId();
        em.clear();
        assertEquals(1, em.find(Curso.class, cursoId).getMatriculas().size());
        assertEquals(1, em.find(Estudiante.class, estudianteId).getMatriculas().size());
    }

    @Test
    void impideDuplicarLaMatriculaDeUnEstudianteEnUnCurso() {
        Matricula primera = matricula();
        assertThrows(PersistenceException.class, () -> {
            em.persist(new Matricula(primera.getEstudiante(), primera.getCurso(), LocalDate.now()));
            em.flush();
        });
    }

    @Test
    void impideVincularDosCuentasAlMismoEstudiante() {
        Estudiante estudiante = estudiante();
        String hash = new BCryptPasswordEncoder().encode("clave-de-prueba");
        Usuario primero = new Usuario("usuario-primero", hash, Rol.ESTUDIANTE);
        primero.vincularEstudiante(estudiante);
        em.persist(primero);
        em.flush();
        assertThrows(PersistenceException.class, () -> {
            Usuario segundo = new Usuario("usuario-segundo", hash, Rol.ESTUDIANTE);
            segundo.vincularEstudiante(estudiante);
            em.persist(segundo);
            em.flush();
        });
    }

    @Test
    void rechazaMatriculasConReferenciasInexistentes() {
        assertThrows(PersistenceException.class, () -> em.createNativeQuery(
                "INSERT INTO matriculas (curso_id, estudiante_id, fecha, estado) "
                + "VALUES (-1, -1, CURRENT_DATE, 'ACTIVA')").executeUpdate());
    }

    @Test
    void rechazaLaEdicionConUnaVersionObsoleta() {
        Curso obsoleto = curso();
        em.flush();
        assertNotNull(obsoleto.getVersion());
        Long versionInicial = obsoleto.getVersion();
        em.detach(obsoleto);
        Curso actual = em.find(Curso.class, obsoleto.getId());
        actual.setCupos(35);
        em.flush();
        assertTrue(actual.getVersion() > versionInicial);
        obsoleto.setCupos(20);
        assertThrows(OptimisticLockException.class, () -> {
            em.merge(obsoleto);
            em.flush();
        });
    }
}
