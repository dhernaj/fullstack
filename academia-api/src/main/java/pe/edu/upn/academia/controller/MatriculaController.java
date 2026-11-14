package pe.edu.upn.academia.controller;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pe.edu.upn.academia.dto.MatriculaRequest;
import pe.edu.upn.academia.dto.MatriculaResponse;
import pe.edu.upn.academia.service.MatriculaService;

@Tag(name = "Matrículas", description = "Inscripción y retiro de matrículas")
@RestController
@RequestMapping("/api/v1/matriculas")
public class MatriculaController {
    private final MatriculaService service;
    public MatriculaController(MatriculaService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public ResponseEntity<MatriculaResponse> crear(@Valid @RequestBody MatriculaRequest request) {
        MatriculaResponse creada = service.matricular(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(uri).body(creada);
    }

    @PatchMapping("/{id}/retiro")
    @PreAuthorize("hasRole('ADMIN')")
    public MatriculaResponse retirar(@PathVariable Long id) { return service.retirar(id); }
}
