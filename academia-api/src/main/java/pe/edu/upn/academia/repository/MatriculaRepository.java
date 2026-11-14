package pe.edu.upn.academia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.edu.upn.academia.model.EstadoMatricula;
import pe.edu.upn.academia.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    boolean existsByCursoId(Long cursoId);

    boolean existsByEstudianteIdAndCursoId(Long estudianteId, Long cursoId);

    long countByCursoIdAndEstado(Long cursoId, EstadoMatricula estado);

    // Consulta sin fetch, conservada para comparar las estrategias de carga.
    List<Matricula> findByCursoId(Long cursoId);

    // Versión optimizada: JOIN FETCH trae todo en una sola consulta
    @Query("""
           select m from Matricula m
           join fetch m.estudiante
           join fetch m.curso
           where m.curso.id = :cursoId
           order by m.estudiante.apellidos
           """)
    List<Matricula> findByCursoConDetalle(@Param("cursoId") Long cursoId);

    // Matrículas del estudiante con las relaciones necesarias para el DTO.
    @Query("""
           select m from Matricula m
           join fetch m.estudiante
           join fetch m.curso
           where m.estudiante.id = :estudianteId
           order by m.fecha desc
           """)
    List<Matricula> findByEstudianteConDetalle(@Param("estudianteId") Long estudianteId);
}
