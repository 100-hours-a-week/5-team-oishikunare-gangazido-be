package org.example.gangazido_be.pet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

	@Bean
	public String uploadDir(@Value("${app.pet.upload.dir}") String uploadDir) {
		return uploadDir;
	}
}
