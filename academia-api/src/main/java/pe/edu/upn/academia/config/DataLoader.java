package pe.edu.upn.academia.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.upn.academia.model.Curso;
import pe.edu.upn.academia.repository.CursoRepository;

/** Carga los cursos de ejemplo únicamente si la tabla está vacía. */
@Component
@Profile("!prod")
@Order(10)
public class DataLoader implements CommandLineRunner {

    private final CursoRepository repository;

    public DataLoader(CursoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(new Curso("SIS-101", "Fundamentos de Programación", 4, "Ana Torres", 40));
        repository.save(new Curso("SIS-205", "Base de Datos", 4, "Luis Pérez", 35));
        repository.save(new Curso("SIS-310", "Desarrollo Web Full Stack", 5, "Carla Ramos", 30));
    }
}
