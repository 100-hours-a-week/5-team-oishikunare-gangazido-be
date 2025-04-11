package org.example.gangazido_be.email.controller;

import org.example.gangazido_be.config.RateLimitConfig;
import org.example.gangazido_be.email.dto.EmailRequestDto;
import org.example.gangazido_be.email.dto.EmailVerifyRequestDto;
import org.example.gangazido_be.email.service.EmailAuthService;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailAuthController {

	private final EmailAuthService emailAuthService;
	private final RateLimitConfig rateLimitConfig;

	@PostMapping("/send")
	public ResponseEntity<?> sendAuthCode(@RequestBody EmailRequestDto dto) {
		String ip = request.getRemoteAddr();
		Bucket bucket = rateLimitConfig.getEmailAuthBucket(ip); // ⬅️ email 전송용 버킷

		if (!bucket.tryConsume(1)) {
			return ResponseEntity.status(429).body(
				Map.of("code", 429, "message", "too_many_requests")
			);
		}
		emailAuthService.sendAuthCode(dto.email());

		return ResponseEntity.ok(Map.of("code", 200, "message", "send_email_success"));
	}

	@PostMapping("/verify")
	public ResponseEntity<?> verifyAuthCode(@RequestBody EmailVerifyRequestDto dto) {
		boolean result = emailAuthService.verifyCode(dto.email(), dto.code());
		if (result) {
			return ResponseEntity.ok(Map.of("code", 200, "message", "verify_email_success"));
		} else {
			return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "invalid_email_or_code"));
		}
	}
}
