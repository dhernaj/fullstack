package pe.edu.upn.academia.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upn.academia.dto.MatriculaRequest;
import pe.edu.upn.academia.model.*;
import pe.edu.upn.academia.repository.*;
import pe.edu.upn.academia.service.MatriculaService;

/** Cuentas y datos ficticios para las pruebas locales del laboratorio. */
@Component
@Order(20)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataLoader implements CommandLineRunner {
    private final UsuarioRepository usuarios;
    private final EstudianteRepository estudiantes;
    private final CursoRepository cursos;
    private final MatriculaRepository matriculas;
    private final MatriculaService service;
    private final PasswordEncoder encoder;
    private final Environment env;

    public DemoDataLoader(UsuarioRepository usuarios, EstudianteRepository estudiantes,
                          CursoRepository cursos, MatriculaRepository matriculas,
                          MatriculaService service, PasswordEncoder encoder, Environment env) {
        this.usuarios=usuarios; this.estudiantes=estudiantes; this.cursos=cursos;
        this.matriculas=matriculas; this.service=service; this.encoder=encoder; this.env=env;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Estudiante primero = estudiantes.findByCodigoIgnoreCase("N00123456").orElseGet(() ->
                estudiantes.save(new Estudiante("N00123456", "Ana", "Torres", "ana.demo@example.test")));
        estudiantes.findByCodigoIgnoreCase("N00987654").orElseGet(() ->
                estudiantes.save(new Estudiante("N00987654", "Luis", "Pérez", "luis.demo@example.test")));
        cuenta("admin", Rol.ADMIN, "LAB_ADMIN_PASSWORD", null);
        cuenta("docente", Rol.DOCENTE, "LAB_DOCENTE_PASSWORD", null);
        cuenta("estudiante", Rol.ESTUDIANTE, "LAB_ESTUDIANTE_PASSWORD", primero);
        for (String codigo : new String[]{"SIS-101", "SIS-310"}) {
            Curso curso = cursos.findByCodigoIgnoreCase(codigo).orElseThrow();
            if (!matriculas.existsByEstudianteIdAndCursoId(primero.getId(), curso.getId())) {
                service.matricular(new MatriculaRequest(primero.getId(), curso.getId()));
            }
        }
    }

    private void cuenta(String nombre, Rol rol, String variable, Estudiante estudiante) {
        if (usuarios.findByUsername(nombre).isPresent()) return;
        String password = env.getRequiredProperty(variable);
        if (password.length() < 12) throw new IllegalArgumentException(variable+" debe tener al menos 12 caracteres");
        Usuario usuario = new Usuario(nombre, encoder.encode(password), rol);
        if (estudiante != null) usuario.vincularEstudiante(estudiante);
        usuarios.save(usuario);
    }
}
