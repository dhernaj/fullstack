package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.model.Curso;
import pe.edu.upn.academia.repository.CursoRepository;

@SpringBootTest
@Transactional // Cada prueba se revierte; no deja cursos de ejemplo.
class CursoRepositoryTests {
    @Autowired
    private CursoRepository repository;

    @Test
    void guardaBuscaSinDistinguirMayusculasYElimina() {
        Curso curso = repository.saveAndFlush(
                new Curso("TST-801", "Programación", 4, "Prueba local", 30));
        assertNotNull(curso.getId());
        Curso encontrado = repository.findByCodigoIgnoreCase("tst-801").orElseThrow();
        assertEquals(curso.getId(), encontrado.getId());
        assertEquals("Programación", encontrado.getNombre());
        assertTrue(repository.findByCodigoIgnoreCase("TST-899").isEmpty());
        assertTrue(repository.findById(curso.getId()).isPresent());
        repository.deleteById(curso.getId());
        repository.flush();
        assertFalse(repository.existsById(curso.getId()));
    }

    @Test
    void listaSoloActivosOrdenadosPorId() {
        Curso primero = repository.save(new Curso("TST-802", "Primero", 3, "Prueba", 20));
        Curso inactivo = new Curso("TST-803", "Inactivo", 3, "Prueba", 20);
        inactivo.setActivo(false);
        repository.save(inactivo);
        Curso ultimo = repository.save(new Curso("TST-804", "Último", 3, "Prueba", 20));
        repository.flush();
        List<Curso> activos = repository.findByActivoTrueOrderByIdAsc();
        List<Long> ids = activos.stream().map(Curso::getId).toList();
        assertTrue(activos.stream().allMatch(Curso::isActivo));
        assertTrue(ids.contains(primero.getId()));
        assertTrue(ids.contains(ultimo.getId()));
        assertFalse(ids.contains(inactivo.getId()));
        assertEquals(ids.stream().sorted().toList(), ids);
    }
}
