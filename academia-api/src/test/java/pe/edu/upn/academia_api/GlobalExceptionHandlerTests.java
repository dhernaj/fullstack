package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import pe.edu.upn.academia.exception.GlobalExceptionHandler;

class GlobalExceptionHandlerTests {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noExponeDetallesDelErrorInterno() {
        var respuesta = handler.manejarGeneral(new IllegalStateException("detalle-interno-de-prueba"));
        assertEquals(500, respuesta.getStatus());
        assertEquals("Error interno", respuesta.getTitle());
        assertEquals("Ocurrió un error inesperado. Intente más tarde.", respuesta.getDetail());
        assertFalse(respuesta.toString().contains("detalle-interno-de-prueba"));
        assertFalse(respuesta.toString().contains("IllegalStateException"));
        assertNull(respuesta.getProperties());
    }

    @Test
    void conflictoDeIntegridadNoExponeSqlNiNombreDeRestriccion() {
        var respuesta = handler.manejarIntegridad(
                new DataIntegrityViolationException("INSERT INTO cursos: restriccion_interna"));
        assertEquals(409, respuesta.getStatus());
        assertEquals("La operación viola una restricción de datos", respuesta.getDetail());
        assertFalse(respuesta.toString().contains("INSERT INTO"));
        assertFalse(respuesta.toString().contains("restriccion_interna"));
    }
}
