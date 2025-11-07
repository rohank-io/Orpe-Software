package com.orpe.consultants.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder
            .modules(new JavaTimeModule()) // registers Java 8 date/time support
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // output ISO format not timestamps
    }
}


