package pe.edu.upn.academia_api;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import pe.edu.upn.academia.security.JwtService;

@SpringBootTest
class JwtServiceTests {
    @Autowired private JwtService service;
    @Autowired private JwtEncoder encoder;
    @Autowired private JwtDecoder decoder;

    @Test
    void emiteUnTokenValidableConSoloLosDatosNecesarios() {
        var auth = UsernamePasswordAuthenticationToken.authenticated("usuario-prueba", "no-exponer",
                List.of(new SimpleGrantedAuthority("ROLE_DOCENTE")));
        var respuesta = service.generar(auth);
        var jwt = decoder.decode(respuesta.accessToken());
        assertEquals("Bearer", respuesta.tokenType());
        assertEquals(3600, respuesta.expiresIn());
        assertEquals("usuario-prueba", jwt.getSubject());
        assertEquals(List.of("DOCENTE"), jwt.getClaimAsStringList("roles"));
        assertEquals(Set.of("iss", "sub", "iat", "exp", "roles"), jwt.getClaims().keySet());
        assertEquals("HS256", jwt.getHeaders().get("alg"));
    }

    private String token(String issuer, Instant expiracion) {
        var claims = JwtClaimsSet.builder().issuer(issuer).subject("usuario-prueba")
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiracion)
                .claim("roles", List.of("ADMIN")).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void rechazaUnTokenVencido() {
        assertThrows(JwtValidationException.class,
                () -> decoder.decode(token("academia-api", Instant.now().minusSeconds(300))));
    }

    @Test
    void rechazaUnTokenFirmadoPeroDeOtroEmisor() {
        assertThrows(JwtValidationException.class,
                () -> decoder.decode(token("otro-emisor", Instant.now().plusSeconds(300))));
    }
}
