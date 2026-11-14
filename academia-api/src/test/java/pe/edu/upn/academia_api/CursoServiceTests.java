package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.dto.CursoRequest;
import pe.edu.upn.academia.dto.CursoResponse;
import pe.edu.upn.academia.exception.ConflictoException;
import pe.edu.upn.academia.exception.RecursoNoEncontradoException;
import pe.edu.upn.academia.repository.CursoRepository;
import pe.edu.upn.academia.service.CursoService;

@SpringBootTest
@Transactional
class CursoServiceTests {
    @Autowired private CursoService service;
    @Autowired private CursoRepository repository;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void aislarDatosDeLaPrueba() {
        // La transacción de prueba revierte este borrado y conserva los ejemplos.
        repository.deleteAll();
        repository.flush();
    }

    private CursoRequest request(String codigo) {
        return new CursoRequest(codigo, "  Programación  ", 4, "  Ana Torres  ", 30);
    }

    private void recargarDesdeBase() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void creaCursoYRecortaEspacios() {
        CursoResponse creado = service.crear(request("TST-901"));
        recargarDesdeBase();
        assertEquals(creado, service.obtener(creado.id()));
        assertEquals("Programación", creado.nombre());
        assertEquals("Ana Torres", creado.docente());
        assertTrue(creado.activo());
    }

    @Test
    void rechazaCodigoDuplicadoSinDistinguirMayusculas() {
        service.crear(request("TST-902"));
        assertThrows(ConflictoException.class, () -> service.crear(request("tst-902")));
    }

    @Test
    void permiteActualizarConElPropioCodigo() {
        CursoResponse creado = service.crear(request("TST-903"));
        service.actualizar(creado.id(), new CursoRequest(
                "TST-903", "  Base de Datos  ", 6, "  Luis Pérez  ", 60));
        recargarDesdeBase();
        CursoResponse actualizado = service.obtener(creado.id());
        assertEquals("Base de Datos", actualizado.nombre());
        assertEquals("Luis Pérez", actualizado.docente());
        assertEquals(6, actualizado.creditos());
        assertEquals(60, actualizado.cupos());
    }

    @Test
    void rechazaCambiarAlCodigoDeOtroCurso() {
        service.crear(request("TST-904"));
        CursoResponse segundo = service.crear(request("TST-905"));
        assertThrows(ConflictoException.class,
                () -> service.actualizar(segundo.id(), request("TST-904")));
    }

    @Test
    void informaCursoInexistenteEnCadaOperacion() {
        long id = Long.MAX_VALUE;
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(id));
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizar(id, request("TST-906")));
        assertThrows(RecursoNoEncontradoException.class, () -> service.actualizarCupos(id, 35));
        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(id));
    }

    @Test
    void filtraActivosPorDocenteYCreditosMinimos() {
        CursoResponse primero = service.crear(request("TST-907"));
        service.crear(new CursoRequest("TST-908", "Otro", 3, "Ana Torres", 20));
        CursoResponse inactivo = service.crear(request("TST-909"));
        service.eliminar(inactivo.id());
        recargarDesdeBase();
        var filtrados = service.listar("ANA", 4, PageRequest.of(0, 10)).contenido();
        assertEquals(1, filtrados.size());
        assertEquals(primero.id(), filtrados.get(0).id());
        assertEquals(2, service.listar(null, null, PageRequest.of(0, 10)).contenido().size());
        assertTrue(service.listar("No existe", null, PageRequest.of(0, 10)).contenido().isEmpty());
    }

    @Test
    void actualizaSoloLosCupos() {
        CursoResponse creado = service.crear(request("TST-910"));
        service.actualizarCupos(creado.id(), 35);
        recargarDesdeBase();
        assertEquals(new CursoResponse(creado.id(), creado.codigo(), creado.nombre(),
                creado.creditos(), creado.docente(), 35, creado.activo()), service.obtener(creado.id()));
    }

    @Test
    void eliminacionLogicaConservaElRegistroYEsIdempotente() {
        CursoResponse creado = service.crear(request("TST-911"));
        service.eliminar(creado.id());
        service.eliminar(creado.id());
        recargarDesdeBase();
        assertTrue(repository.existsById(creado.id()));
        assertFalse(service.obtener(creado.id()).activo());
        assertTrue(service.listar(null, null, PageRequest.of(0, 10)).contenido().isEmpty());
    }
}
