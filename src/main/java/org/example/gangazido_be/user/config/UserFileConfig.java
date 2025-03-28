// package org.example.gangazido_be.user.config;
//
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.multipart.MultipartResolver;
// import org.springframework.web.multipart.support.StandardServletMultipartResolver;
// import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
// import java.nio.file.Path;
// import java.nio.file.Paths;
//
// @Configuration
// public class UserFileConfig implements WebMvcConfigurer {
//
// 	@Value("${app.user.upload.dir:user-uploads}")
// 	private String uploadDir;
//
// 	@Bean
// 	public MultipartResolver multipartResolver() {
// 		return new StandardServletMultipartResolver();
// 	}
//
// 	@Override
// 	public void addResourceHandlers(ResourceHandlerRegistry registry) {
// 		// 업로드된 파일에 접근할 수 있는 URL 경로 설정
// 		Path uploadPath = Paths.get(uploadDir);
// 		String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();
//
// 		registry.addResourceHandler("/upload/user/**")
// 			.addResourceLocations("file:" + uploadAbsolutePath + "/");
// 	}
// }
