// 프로덕션 환경으로 이동하면서 도메인 구조가 변경될 경우
// 프론트엔드와 백엔드를 분리하여 다른 도메인에서 호스팅할 경우
// CORS 조절 필요


// package org.example.gangazido_be.user.config;
//
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.servlet.config.annotation.CorsRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
// @Configuration
// public class UserCorsConfig implements WebMvcConfigurer {
// 	@Override
// 	public void addCorsMappings(CorsRegistry registry) {
// 		registry.addMapping("/**")
// 			.allowedOrigins("http://localhost:8080") // 프론트엔드 주소
// 			.allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
// 			.allowCredentials(true) // 이 설정이 중요합니다. 쿠키 허용
// 			.maxAge(3600);
// 	}
// }
