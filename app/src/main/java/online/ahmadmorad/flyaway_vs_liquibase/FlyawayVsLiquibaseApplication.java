package online.ahmadmorad.flyaway_vs_liquibase;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlyawayVsLiquibaseApplication {
	public static void main(String[] args) {
		SpringApplication.run(FlyawayVsLiquibaseApplication.class, args);
	}

	@PostConstruct
	public void checkLiquibaseFiles() {
		try {
			System.out.println("Checking files...");
			System.out.println("MASTER: " + getClass().getResource("/db/changelog/master.xml"));
			System.out.println("V1: " + getClass().getResource("/db/changelog/V1__init.xml"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}