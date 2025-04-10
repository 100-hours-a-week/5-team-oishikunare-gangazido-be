package org.example.gangazido_be.email.controller;

import org.example.gangazido_be.email.dto.EmailRequestDto;
import org.example.gangazido_be.email.dto.EmailVerifyRequestDto;
import org.example.gangazido_be.email.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailAuthController {

	private final EmailAuthService emailAuthService;

	@PostMapping("/send")
	public ResponseEntity<?> sendAuthCode(@RequestBody EmailRequestDto dto) {
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
