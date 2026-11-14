package pe.edu.upn.academia_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories("pe.edu.upn.academia.repository")
@EntityScan("pe.edu.upn.academia.model")
@SpringBootApplication(scanBasePackages = "pe.edu.upn.academia")
public class AcademiaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcademiaApiApplication.class, args);
	}

}
