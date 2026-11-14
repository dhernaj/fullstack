package pe.edu.upn.academia_api;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upn.academia.dto.CursoRequest;
import pe.edu.upn.academia.exception.GlobalExceptionHandler;
import pe.edu.upn.academia.model.Curso;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

class ErroresHttpTests {
    private MockMvc mvc;

    @BeforeEach
    void prepararMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void credencialesIncorrectasDevuelven401Generico() throws Exception {
        mvc.perform(get("/prueba/errores/credenciales"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"))
                .andExpect(content().string(not(containsString("detalle-credencial"))));
    }

    @Test
    void usuarioDesconocidoDevuelveElMismoMensaje() throws Exception {
        mvc.perform(get("/prueba/errores/usuario"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"))
                .andExpect(content().string(not(containsString("nombre-interno"))));
    }

    @Test
    void accesoDenegadoDevuelve403EnLugarDe500() throws Exception {
        mvc.perform(get("/prueba/errores/acceso"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("No tiene permisos para esta operación"));
    }

    @Test
    void versionObsoletaDevuelve409SinIdentificadoresInternos() throws Exception {
        mvc.perform(get("/prueba/errores/concurrencia"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflicto de concurrencia"))
                .andExpect(jsonPath("$.detail").value(
                        "El recurso fue modificado por otro usuario. Vuelva a cargarlo e intente de nuevo."))
                .andExpect(content().string(not(containsString("pe.edu.upn"))));
    }

    @Test
    void errorInesperadoDevuelve500SinTraza() throws Exception {
        mvc.perform(get("/prueba/errores/general"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("detalle-interno-http"))));
    }

    @Test
    void jsonMalFormadoConservaEl400DelFramework() throws Exception {
        mvc.perform(post("/prueba/errores/entrada").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validacionMantieneLosErroresPorCampo() throws Exception {
        mvc.perform(post("/prueba/errores/entrada").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"SIS-400","nombre":"","creditos":9,"docente":"Ana","cupos":30}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio"))
                .andExpect(jsonPath("$.errores.creditos").value("Máximo 6 créditos"));
    }

    @Test
    void metodoNoPermitidoConservaEl405DelFramework() throws Exception {
        mvc.perform(get("/prueba/errores/entrada"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    // Solo existe en las pruebas; no se registra en el servidor de la aplicación.
    @RestController
    @RequestMapping("/prueba/errores")
    static class ControladorDePrueba {
        @GetMapping("/credenciales") void credenciales() { throw new BadCredentialsException("detalle-credencial"); }
        @GetMapping("/usuario") void usuario() { throw new UsernameNotFoundException("nombre-interno"); }
        @GetMapping("/acceso") void acceso() { throw new AccessDeniedException("detalle-permiso"); }
        @GetMapping("/concurrencia") void concurrencia() { throw new ObjectOptimisticLockingFailureException(Curso.class, 123L); }
        @GetMapping("/general") void general() { throw new IllegalStateException("detalle-interno-http"); }
        @PostMapping("/entrada") CursoRequest entrada(@Valid @RequestBody CursoRequest request) { return request; }
    }
}
