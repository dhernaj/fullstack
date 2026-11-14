package pe.edu.upn.academia.model;
 
import java.util.ArrayList;
import java.util.List;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "estudiantes")
public class Estudiante {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 10)
    private String codigo;          // código universitario, p. ej. N00123456
 
    @Column(nullable = false, length = 60)
    private String nombres;
 
    @Column(nullable = false, length = 60)
    private String apellidos;
 
    @Column(nullable = false, unique = true, length = 120)
    private String email;
 
    @OneToMany(mappedBy = "estudiante")
    private List<Matricula> matriculas = new ArrayList<>();
 
    protected Estudiante() {
    }
 
    public Estudiante(String codigo, String nombres, String apellidos, String email) {
        this.codigo = codigo;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
    }
 
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<Matricula> getMatriculas() { return matriculas; }
 
    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
