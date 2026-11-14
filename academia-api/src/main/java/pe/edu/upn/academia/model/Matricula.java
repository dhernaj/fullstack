package pe.edu.upn.academia.model;
 
import java.time.LocalDate;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
 
/**
 * Tabla intermedia con atributos propios: resuelve la relación N:M
 * entre Estudiante y Curso. Es la dueña de ambas relaciones (tiene las FK).
 */
@Entity
@Table(name = "matriculas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"estudiante_id", "curso_id"}))
public class Matricula {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;
 
    @Column(nullable = false)
    private LocalDate fecha;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoMatricula estado = EstadoMatricula.ACTIVA;
 
    protected Matricula() {
    }
 
    public Matricula(Estudiante estudiante, Curso curso, LocalDate fecha) {
        this.estudiante = estudiante;
        this.curso = curso;
        this.fecha = fecha;
    }
 
    public Long getId() { return id; }
    public Estudiante getEstudiante() { return estudiante; }
    public Curso getCurso() { return curso; }
    public LocalDate getFecha() { return fecha; }
    public EstadoMatricula getEstado() { return estado; }
 
    public void retirar() {
        this.estado = EstadoMatricula.RETIRADA;
    }
}
