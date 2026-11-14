package pe.edu.upn.academia.model;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
 
@Entity
@Table(name = "usuarios")
public class Usuario {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 50)
    private String username;
 
    @Column(nullable = false, length = 100)
    private String password;        // siempre el hash BCrypt, nunca el texto plano
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Rol rol;
 
    // Relaciona la cuenta con su registro académico.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", unique = true)
    private Estudiante estudiante;
 
    protected Usuario() {
    }
 
    public Usuario(String username, String passwordHash, Rol rol) {
        this.username = username;
        this.password = passwordHash;
        this.rol = rol;
    }
 
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Rol getRol() { return rol; }
    public Estudiante getEstudiante() { return estudiante; }
    public void vincularEstudiante(Estudiante estudiante) { this.estudiante = estudiante; }
}
