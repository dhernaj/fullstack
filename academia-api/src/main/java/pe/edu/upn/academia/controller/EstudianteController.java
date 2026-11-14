package pe.edu.upn.academia.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import pe.edu.upn.academia.dto.EstudianteRequest;
import pe.edu.upn.academia.dto.EstudianteResponse;
import pe.edu.upn.academia.dto.MatriculaResponse;
import pe.edu.upn.academia.dto.PaginaResponse;
import pe.edu.upn.academia.service.EstudianteService;
import pe.edu.upn.academia.service.MatriculaService;

@RestController
@RequestMapping("/api/v1/estudiantes")
public class EstudianteController {

    private final EstudianteService service;
    private final MatriculaService matriculaService;

    public EstudianteController(EstudianteService service, MatriculaService matriculaService) {
        this.service = service;
        this.matriculaService = matriculaService;
    }

    /**
     * ADMIN y DOCENTE pueden consultar cualquier estudiante;
     * un ESTUDIANTE solo sus propias matrículas.
     */
    @GetMapping("/{id}/matriculas")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE') or @accesoEstudiante.esPropio(authentication, #id)")
    public List<MatriculaResponse> matriculas(@PathVariable Long id) {
        return matriculaService.listarPorEstudiante(id);
    }

    @GetMapping
    public PaginaResponse<EstudianteResponse> listar(@PageableDefault(size = 10, sort = "apellidos") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public EstudianteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EstudianteResponse> crear(@Valid @RequestBody EstudianteRequest request) {
        EstudianteResponse creado = service.crear(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(uri).body(creado);
    }
}
