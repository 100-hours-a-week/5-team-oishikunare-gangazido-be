package org.example.gangazido_be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {	// 아래 코드대로하면 security에서 중단 예정이라해서 레첼이 변경한 코드
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:3000", "https://www.gangazido.com"));
		config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);

		return source;
	}
}

// package org.example.gangazido_be.config;
//
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
// @Configuration	// 스프링 설정 클래스임을 명시, 제리 추가
// public class CorsConfig {
//
// 	// 스프링이 이 메서드를 Bean으로 관리하도록 설정
// 	// 화이팅!
// 	@Bean
// 	public WebMvcConfigurer corsConfigurer() {
// 		return new WebMvcConfigurer() {
// 			@Override
// 			public void addCorsMappings(CorsRegistry registry) {
// 				registry.addMapping("/**") // 모든 경로(API 엔드포인트)에 대해 CORS 설정 적용
// 					.allowedOrigins("http://localhost:3000", "https://www.gangazido.com") // 허용할 프론트 주소
// 					.allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS") // 허용할 메서드
// 					.allowCredentials(true) // 세션/쿠키 포함 시 true, 프론트에서 요청 시 withCredentials: true 필요
// 					.allowedHeaders("*");	// 모든 요청 헤더 허용
// 			}
// 		};
// 	}
// }
