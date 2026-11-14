package pe.edu.upn.academia.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.edu.upn.academia.model.Rol;
import pe.edu.upn.academia.model.Usuario;
import pe.edu.upn.academia.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {
    @Mock UsuarioRepository usuarios;

    @Test
    void creaElAdministradorConHashYClaveDeDoceCaracteres() {
        String clave = "Clave123456!";
        var encoder = new BCryptPasswordEncoder(4);
        when(usuarios.findByUsername("admin")).thenReturn(Optional.empty());
        new AdminInitializer(usuarios, encoder, clave).run();
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        Usuario creado = captor.getValue();
        assertThat(creado.getUsername()).isEqualTo("admin");
        assertThat(creado.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(creado.getPassword()).isNotEqualTo(clave);
        assertThat(encoder.matches(clave, creado.getPassword())).isTrue();
    }

    @Test
    void conservaLaCuentaExistenteSinRestablecerSuClave() {
        Usuario existente = new Usuario("admin", "hash-existente", Rol.ADMIN);
        when(usuarios.findByUsername("admin")).thenReturn(Optional.of(existente));
        new AdminInitializer(usuarios, new BCryptPasswordEncoder(4), "").run();
        verify(usuarios, never()).save(any());
        assertThat(existente.getPassword()).isEqualTo("hash-existente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "corta", "12345678901"})
    void noCreaUnaCuentaConClaveAusenteOCorta(String clave) {
        when(usuarios.findByUsername("admin")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new AdminInitializer(usuarios, new BCryptPasswordEncoder(4), clave).run())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_PASSWORD");
        verify(usuarios, never()).save(any());
    }
}
