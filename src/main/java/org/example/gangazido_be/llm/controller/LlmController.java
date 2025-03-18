//llmcontroller
package org.example.gangazido_be.llm.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

import org.example.gangazido_be.llm.model.LlmRequest;
import org.example.gangazido_be.llm.model.LlmResponse;
import org.example.gangazido_be.llm.service.LlmService;

@RestController
@RequestMapping("/v1/llm")
public class LlmController {
	private final LlmService llmService;

	public LlmController(LlmService llmService) {
		this.llmService = llmService;
	}

	// ✅ POST 요청: GPT 응답 생성 후 저장
	@PostMapping("/chat")
	public ResponseEntity<LlmResponse> generateChat(@RequestBody LlmRequest request,
		HttpServletRequest httpServletRequest) {
		String sessionId = extractSessionId(httpServletRequest);
		if (sessionId == null) {
			return ResponseEntity.badRequest().body(new LlmResponse("session_error", "❌ 세션 ID가 없습니다."));
		}

		return ResponseEntity.ok(
			llmService.generateChat(
				httpServletRequest,  // ✅ 세션 ID를 반영하도록 수정
				request.getLatitude(),
				request.getLongitude()));
	}

	// ✅ 세션 ID 추출
	private String extractSessionId(HttpServletRequest request) {
		String cookie = request.getHeader("Cookie");
		if (cookie == null || !cookie.contains("connect.sid=")) {
			return null;
		}
		return cookie.split("connect.sid=")[1].split(";")[0];
	}
}
