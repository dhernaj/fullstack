package pe.edu.upn.academia.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import javax.sql.DataSource;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.model.*;
import pe.edu.upn.academia.repository.*;
import pe.edu.upn.academia_api.AcademiaApiApplication;

@SpringBootTest(classes = AcademiaApiApplication.class)
@AutoConfigureMockMvc
@Transactional
class CursoApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired EstudianteRepository estudiantes;
    @Autowired PasswordEncoder encoder;
    @Autowired DataSource dataSource;
    private Long propio;
    private Long otro;
    private static final String CLAVE = "clave-exclusiva-de-test-2026";

    @BeforeEach
    void prepararCuentasSoloDePrueba() {
        Estudiante estudiante = estudiantes.save(new Estudiante("N00000011", "Ana", "Uno", "ana-test@example.test"));
        propio = estudiante.getId();
        otro = estudiantes.save(new Estudiante("N00000012", "Luis", "Dos", "luis-test@example.test")).getId();
        usuarios.save(new Usuario("admin-test", encoder.encode(CLAVE), Rol.ADMIN));
        Usuario cuenta = new Usuario("estudiante-test", encoder.encode(CLAVE), Rol.ESTUDIANTE);
        cuenta.vincularEstudiante(estudiante);
        usuarios.save(cuenta);
    }

    private String token(String usuario) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, CLAVE)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.accessToken");
    }

    @Test
    void usaH2SinNecesitarPostgreSQLNiClavesLocales() throws Exception {
        try (var conexion = dataSource.getConnection()) {
            assertThat(conexion.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(conexion.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
        }
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/v1/cursos")).andExpect(status().isUnauthorized());
    }

    @Test
    void credencialesInvalidasResponden401() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin-test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void estudianteAutenticadoListaCursosPaginados() throws Exception {
        mvc.perform(get("/api/v1/cursos?size=2").header("Authorization", "Bearer " + token("estudiante-test")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(2))
                .andExpect(jsonPath("$.tamanio").value(2));
    }

    @Test
    void estudianteNoPuedeCrearCursos() throws Exception {
        mvc.perform(post("/api/v1/cursos").header("Authorization", "Bearer " + token("estudiante-test"))
                        .contentType(MediaType.APPLICATION_JSON).content(curso("SIS-901")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaCursoCon201YLocation() throws Exception {
        mvc.perform(post("/api/v1/cursos").header("Authorization", "Bearer " + token("admin-test"))
                        .contentType(MediaType.APPLICATION_JSON).content(curso("SIS-902")))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.codigo").value("SIS-902"));
    }

    @Test
    void datosInvalidosResponden400ConErroresPorCampo() throws Exception {
        mvc.perform(post("/api/v1/cursos").header("Authorization", "Bearer " + token("admin-test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"x\",\"nombre\":\"\",\"creditos\":9,\"docente\":\"A\",\"cupos\":10}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.codigo").exists())
                .andExpect(jsonPath("$.errores.creditos").exists());
    }

    @Test
    void estudianteSoloVeSusPropiasMatriculas() throws Exception {
        String jwt = token("estudiante-test");
        mvc.perform(get("/api/v1/estudiantes/{id}/matriculas", propio).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/estudiantes/{id}/matriculas", otro).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthEsPublicoYNoExponeDetallesInternos() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    void swaggerUiEsPublicoYSeSirveSuPagina() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void contratoPublicoDescribeJwtPaginacionYLoginSinToken() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Academia UPN API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].get.parameters[*].name")
                        .value(hasItems("page", "size", "sort", "docente", "creditosMin")))
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].post.responses['400'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.paths['/api/v1/cursos'].post.responses['409'].content['application/problem+json'].schema['$ref']")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.tags[*].name").value(hasItems("Cursos", "Estudiantes", "Matrículas", "Autenticación")));
    }

    @Test
    void otrosEndpointsDeActuatorNoQuedanAbiertos() throws Exception {
        mvc.perform(get("/actuator/env").header("Authorization", "Bearer " + token("admin-test")))
                .andExpect(status().isForbidden());
    }

    @Test
    void estudianteSigueSinAccesoAResumenNiMatriculasDeCurso() throws Exception {
        String jwt = token("estudiante-test");
        mvc.perform(get("/api/v1/cursos/resumen").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/cursos/1/matriculas").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    private static String curso(String codigo) {
        return "{\"codigo\":\"%s\",\"nombre\":\"Curso de prueba\",\"creditos\":3,\"docente\":\"QA\",\"cupos\":20}".formatted(codigo);
    }
}
