package pe.edu.upn.academia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pe.edu.upn.academia.dto.CursoResumen;
import pe.edu.upn.academia.model.Curso;

/**
 * Spring Data JPA genera la implementación en tiempo de ejecución.
 */
public interface CursoRepository extends JpaRepository<Curso, Long> {

    // Consultas derivadas del nombre del método
    Optional<Curso> findByCodigoIgnoreCase(String codigo);

    // Bloquea el curso mientras se comprueba y ocupa una plaza.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Curso c where c.id = :id")
    Optional<Curso> findByIdParaMatricula(@Param("id") Long id);

    Page<Curso> findByDocenteContainingIgnoreCase(String docente, Pageable pageable);

    // JPQL con parámetros con nombre (nunca concatenar texto del usuario)
    @Query("""
           select c from Curso c
           where c.activo = true and c.creditos >= :minimo
           order by c.creditos desc, c.nombre
           """)
    List<Curso> buscarActivosConCreditosMinimos(@Param("minimo") int minimo);

    // Proyección a DTO con agregación: un solo SELECT con GROUP BY
    @Query("""
           select new pe.edu.upn.academia.dto.CursoResumen(
                  c.id, c.codigo, c.nombre, c.cupos, count(m))
           from Curso c left join c.matriculas m
                on m.estado = pe.edu.upn.academia.model.EstadoMatricula.ACTIVA
           group by c.id, c.codigo, c.nombre, c.cupos
           order by c.nombre
           """)
    List<CursoResumen> resumenOcupacion();
    List<Curso> findByActivoTrueOrderByIdAsc();

    Page<Curso> findByActivoTrueAndDocenteContainingIgnoreCaseAndCreditosGreaterThanEqual(
            String docente, int creditos, Pageable pageable);
}
