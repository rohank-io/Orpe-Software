package com.orpe.consultants;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "com.orpe.consultants.repository")
@EntityScan(basePackages = "com.orpe.consultants.model")
@SpringBootApplication
@ComponentScan(basePackages = {"com.orpe.consultants", "com.orpe.consultants.config"})
public class OrpeConsultantsApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrpeConsultantsApplication.class, args);
	}

}
