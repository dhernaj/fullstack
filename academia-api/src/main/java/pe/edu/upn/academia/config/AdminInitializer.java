package pe.edu.upn.academia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.edu.upn.academia.model.Rol;
import pe.edu.upn.academia.model.Usuario;
import pe.edu.upn.academia.repository.UsuarioRepository;

@Component
@Profile("prod")
public class AdminInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String password;

    public AdminInitializer(UsuarioRepository usuarios, PasswordEncoder encoder,
                            @Value("${app.admin.password:}") String password) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (usuarios.findByUsername("admin").isPresent()) return;
        if (password.length() < 12) {
            throw new IllegalStateException("ADMIN_PASSWORD debe tener al menos 12 caracteres para crear el administrador");
        }
        usuarios.save(new Usuario("admin", encoder.encode(password), Rol.ADMIN));
        log.info("Usuario admin creado");
    }
}
