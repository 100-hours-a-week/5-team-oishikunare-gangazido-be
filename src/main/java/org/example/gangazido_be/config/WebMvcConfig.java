package org.example.gangazido_be.config;

import org.example.gangazido_be.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final RateLimitInterceptor rateLimitInterceptor;

	@Autowired
	public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
		this.rateLimitInterceptor = rateLimitInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// 인터셉터 등록 및 적용할 경로 지정
		registry.addInterceptor(rateLimitInterceptor)
			.addPathPatterns("/v1/users/login",
				"/v1/users/signup",
				"/v1/users/check-email",
				"/v1/users/check-nickname",
				"/v1/users/signup/profile-image-upload-url",
				"/v1/users/profile-image-upload-url",
				"/v1/users/profile-image-update");
	}
}
