package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pe.edu.upn.academia.dto.CursoRequest;
import pe.edu.upn.academia.dto.CursoResponse;
import pe.edu.upn.academia.model.Curso;

class CursoDtoTests {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void iniciarValidacion() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidacion() { factory.close(); }

    private Set<String> camposInvalidos(CursoRequest request) {
        return validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void aceptaLosLimitesValidos() {
        assertTrue(camposInvalidos(new CursoRequest("SIS-101", "A", 1, "B", 1)).isEmpty());
        assertTrue(camposInvalidos(new CursoRequest(
                "SIS-999", "A".repeat(100), 6, "B".repeat(80), 60)).isEmpty());
    }

    @Test
    void rechazaCamposObligatoriosVaciosONulos() {
        assertEquals(Set.of("codigo", "nombre", "docente"),
                camposInvalidos(new CursoRequest(null, "  ", 4, "", 30)));
    }

    @Test
    void rechazaFormatosDeCodigoIncorrectos() {
        for (String codigo : new String[]{"sis-101", "SIS101", "SIS-10", "SISS-101"}) {
            assertEquals(Set.of("codigo"), camposInvalidos(
                    new CursoRequest(codigo, "Programación", 4, "Ana", 30)), codigo);
        }
    }

    @Test
    void rechazaNumerosFueraDeRango() {
        assertEquals(Set.of("creditos", "cupos"), camposInvalidos(
                new CursoRequest("SIS-101", "Curso", 0, "Ana", 0)));
        assertEquals(Set.of("creditos", "cupos"), camposInvalidos(
                new CursoRequest("SIS-101", "Curso", 7, "Ana", 61)));
    }

    @Test
    void rechazaTextosQueSuperanElLimite() {
        assertEquals(Set.of("nombre", "docente"), camposInvalidos(
                new CursoRequest("SIS-101", "A".repeat(101), 4, "B".repeat(81), 30)));
    }

    @Test
    void convierteLaEntidadSinPerderAcentosNiEstado() {
        Curso curso = new Curso("SIS-101", "Programación", 4, "Luis Pérez", 30);
        curso.setActivo(false);
        CursoResponse response = CursoResponse.desde(curso);
        assertEquals(new CursoResponse(null, "SIS-101", "Programación", 4,
                "Luis Pérez", 30, false), response);
    }
}
