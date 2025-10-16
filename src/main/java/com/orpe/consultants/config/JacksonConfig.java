package com.orpe.consultants.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Module javaTimeModule() {
        // This registers Java 8 date/time support (LocalDate, LocalDateTime, etc.)
        return new JavaTimeModule();
    }
}

