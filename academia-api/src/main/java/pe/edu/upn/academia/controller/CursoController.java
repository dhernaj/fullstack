package pe.edu.upn.academia.controller;

import java.net.URI;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import pe.edu.upn.academia.dto.CursoResumen;
import pe.edu.upn.academia.dto.MatriculaResponse;
import pe.edu.upn.academia.service.MatriculaService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import pe.edu.upn.academia.dto.PaginaResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pe.edu.upn.academia.dto.CuposRequest;
import pe.edu.upn.academia.dto.CursoRequest;
import pe.edu.upn.academia.dto.CursoResponse;
import pe.edu.upn.academia.service.CursoService;

/** Expone las operaciones de cursos mediante HTTP. */
@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {
    private final CursoService service;

    private final MatriculaService matriculas;

    public CursoController(CursoService service, MatriculaService matriculas) {
        this.service = service;
        this.matriculas = matriculas;
    }

    @GetMapping("/{id}/matriculas")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public List<MatriculaResponse> matriculas(@PathVariable Long id) {
        return matriculas.listarPorCurso(id);
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public List<CursoResumen> resumen() { return service.resumenOcupacion(); }

    @GetMapping
    public PaginaResponse<CursoResponse> listar(@RequestParam(required = false) String docente,
                                      @RequestParam(required = false) Integer creditosMin,
                                      @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return service.listar(docente, creditosMin, pageable);
    }

    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
    @PostMapping
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        CursoResponse creado = service.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id,
                                   @Valid @RequestBody CursoRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/cupos")
    public CursoResponse actualizarCupos(@PathVariable Long id,
                                        @Valid @RequestBody CuposRequest request) {
        return service.actualizarCupos(id, request.cupos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
