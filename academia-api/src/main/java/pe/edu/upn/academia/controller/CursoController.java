package pe.edu.upn.academia.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.ParameterObject;

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
@Tag(name = "Cursos", description = "Catálogo de cursos")
@RestController
@RequestMapping("/api/v1/cursos")
public class CursoController {
    private final CursoService service;

    private final MatriculaService matriculas;

    public CursoController(CursoService service, MatriculaService matriculas) {
        this.service = service;
        this.matriculas = matriculas;
    }

    @Operation(summary = "Consultar matrículas de un curso", description = "Requiere ADMIN o DOCENTE")
    @GetMapping("/{id}/matriculas")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public List<MatriculaResponse> matriculas(@PathVariable Long id) {
        return matriculas.listarPorCurso(id);
    }

    @Operation(summary = "Consultar la ocupación de los cursos", description = "Cuenta solo matrículas activas; requiere ADMIN o DOCENTE")
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
    public List<CursoResumen> resumen() { return service.resumenOcupacion(); }

    @Operation(summary = "Listar cursos paginados", description = "Filtros opcionales por docente y créditos mínimos. El tamaño de página se limita a 50.")
    @GetMapping
    public PaginaResponse<CursoResponse> listar(@RequestParam(required = false) String docente,
                                      @RequestParam(required = false) Integer creditosMin,
                                      @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return service.listar(docente, creditosMin, pageable);
    }

    @Operation(summary = "Obtener un curso por id")
    @ApiResponse(responseCode = "200", description = "Curso encontrado")
    @ApiResponse(responseCode = "404", description = "El curso no existe",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
    @Operation(summary = "Crear un curso", description = "Requiere rol ADMIN")
    @ApiResponse(responseCode = "201", description = "Curso creado; cabecera Location")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Código duplicado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CursoRequest request) {
        CursoResponse creado = service.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Actualizar un curso", description = "Requiere rol ADMIN")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "El curso no existe",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Código duplicado",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id,
                                   @Valid @RequestBody CursoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Actualizar los cupos de un curso", description = "Requiere rol ADMIN")
    @PatchMapping("/{id}/cupos")
    public CursoResponse actualizarCupos(@PathVariable Long id,
                                        @Valid @RequestBody CuposRequest request) {
        return service.actualizarCupos(id, request.cupos());
    }

    @Operation(summary = "Desactivar un curso", description = "Eliminación lógica. Requiere rol ADMIN; no admite cursos con matrículas.")
    @ApiResponse(responseCode = "204", description = "Curso desactivado, sin contenido")
    @ApiResponse(responseCode = "404", description = "El curso no existe",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "El curso tiene matrículas",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
