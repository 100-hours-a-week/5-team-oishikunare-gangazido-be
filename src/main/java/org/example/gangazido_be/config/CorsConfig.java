package org.example.gangazido_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration	// 스프링 설정 클래스임을 명시, 제리 추가
public class CorsConfig {

	// 스프링이 이 메서드를 Bean으로 관리하도록 설정
	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**") // 모든 경로(API 엔드포인트)에 대해 CORS 설정 적용
					.allowedOrigins("http://localhost:3000", "https://www.gangazido.com") // 허용할 프론트 주소
					.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS") // 허용할 메서드
					.allowCredentials(true) // 세션/쿠키 포함 시 true, 프론트에서 요청 시 withCredentials: true 필요
					.allowedHeaders("*");	// 모든 요청 헤더 허용
			}
		};
	}
}
