package org.example.gangazido_be.user.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class UserSecurityConfig {

	@Bean
	public Argon2PasswordEncoder passwordEncoder() {
		// Argon2 설정: saltLength, hashLength, parallelism, memory, iterations
		return new Argon2PasswordEncoder(16, 32, 1, 4096, 1);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource))  // CorsConfig 변경에 따른 레첼 추가 코드
			.csrf(csrf -> csrf.disable())  // API 서버이므로 CSRF 비활성화
			.authorizeHttpRequests( auth -> auth
					.requestMatchers("/**").permitAll()
				// .requestMatchers("/v1/users/signup").permitAll()  // 회원가입 API는 모두 허용
				// .requestMatchers("/v1/users/login").permitAll()   // 로그인 API는 모두 허용
				// .requestMatchers("/v1/users/logout").permitAll()   // 로그아웃 API는 모두 허용
				// .requestMatchers("/v1/users/me").permitAll()  // 사용자 관련 API 허용
				// .requestMatchers("/v1/users/check-email").permitAll()  // 이메일 중복 확인 API 모두 허용
				// .requestMatchers("/v1/users/check-nickname").permitAll()  // 닉네임 중복 확인 API 모두 허용
				// .requestMatchers("/v1/users/me/password").permitAll()  // 비밀번호 변경 API 허용
				// .requestMatchers("/user-uploads/**").permitAll()  // 프로필 이미지 접근은 모두 허용
				// .requestMatchers("/api/system/**").permitAll()    // 시스템 테스트 API
				// .requestMatchers("/swagger-ui/").permitAll()    // Swagger UI 및 API 문서 접근 허용 : 제리 추가
				// .requestMatchers("/swagger-ui.html").permitAll()
				// .requestMatchers("/v3/api-docs/").permitAll()
				// .requestMatchers("/swagger-resources/").permitAll()
				// .requestMatchers("/webjars/").permitAll()
				// .anyRequest().authenticated()  // 나머지 API는 인증 필요
			)
			.formLogin(formLogin -> formLogin.disable())  // 폼 로그인 비활성화
			.httpBasic(httpBasic -> httpBasic.disable());  // HTTP Basic 인증 비활성화

		return http.build();
	}
}
