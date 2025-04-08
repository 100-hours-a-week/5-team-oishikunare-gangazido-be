package org.example.gangazido_be.interceptor;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.gangazido_be.config.RateLimitConfig;
import org.example.gangazido_be.user.dto.UserApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

	private final RateLimitConfig rateLimitConfig;
	private final ObjectMapper objectMapper;
	private final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);

	@Autowired
	public RateLimitInterceptor(RateLimitConfig rateLimitConfig, ObjectMapper objectMapper) {
		this.rateLimitConfig = rateLimitConfig;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("checkstyle:RightCurly")
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}

		String ipAddress = getClientIpAddress(request);
		String requestURI = request.getRequestURI();
		String method = request.getMethod();

		Bucket bucket = null;


		if (requestURI.equals("/v1/users/login") && method.equals("POST")) {
			bucket = rateLimitConfig.getLoginBucket(ipAddress);
		} else if (requestURI.equals("/v1/users/signup") && method.equals("POST")) {
			bucket = rateLimitConfig.getSignupBucket(ipAddress);
		} else if ((requestURI.equals("/v1/users/check-email") || requestURI.equals("/v1/users/check-nickname")) &&
			method.equals("GET")) {
			bucket = rateLimitConfig.getDuplicateCheckBucket(ipAddress);
		} else if ((requestURI.equals("/v1/users/signup/profile-image-upload-url") ||
			requestURI.equals("/v1/users/profile-image-upload-url") ||
			requestURI.equals("/v1/users/profile-image-update")) && method.equals("POST")) {
			bucket = rateLimitConfig.getImageUploadBucket(ipAddress);
		}

		// 버킷이 할당된 경우 (즉, 제한이 필요한 API인 경우) 요청 제한 검사
		if (bucket != null) {
			if (!bucket.tryConsume(1)) {
				logger.warn("요청 제한 초과: IP={}, URI={}", ipAddress, requestURI);

				// UserApiResponse.tooManyRequests() 메서드를 사용하면 더 깔끔하게 구현할 수 있으나,
				// 여기서는 인터셉터에서 직접 응답을 생성해야 하므로 수동으로 구성
				response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				response.setContentType("application/json;charset=UTF-8");  // charset=UTF-8 추가

				ResponseEntity<UserApiResponse<Object>> responseEntity =
					UserApiResponse.error(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수가 제한을 초과했습니다. 잠시 후 다시 시도해주세요.");

				String jsonResponse = objectMapper.writeValueAsString(responseEntity.getBody());
				response.getWriter().write(jsonResponse);
				return false;
			}
		}

		return true;
	}

	// 클라이언트 IP 주소 가져오기
	private String getClientIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");

		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}

		// 여러 IP가 포함된 경우 첫 번째 IP만 사용
		if (ip != null && ip.contains(",")) {
			ip = ip.split(",")[0].trim();
		}

		return ip;
	}
}
