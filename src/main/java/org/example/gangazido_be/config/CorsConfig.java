package org.example.gangazido_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로에 대해
                        .allowedOrigins("http://localhost:3000", "https://www.gangazido.com") // 허용할 프론트 주소
                        .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                        .allowCredentials(true) // 세션/쿠키 포함 시 true
                        .allowedHeaders("*");
            }
        };
    }
}
