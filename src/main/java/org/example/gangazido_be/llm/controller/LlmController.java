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

	private String extractSessionId(HttpServletRequest request) {
		String cookie = request.getHeader("Cookie");

		if (cookie == null) {
			System.err.println("[ERROR] required_session_id: 세션 쿠키가 존재하지 않습니다.");
			return null;
		}

		String[] parts = cookie.split("connect.sid=");
		if (parts.length < 2) {
			System.err.println("[ERROR] invalid_session_id: 세션 쿠키 형식이 올바르지 않습니다.");
			return null;
		}

		String sessionId = parts[1].split(";")[0].trim();

		if (!sessionId.matches("\\d+")) { // 숫자가 아니면 예외 방지
			System.err.println("[ERROR] invalid_session_id: 세션 ID가 숫자가 아닙니다.");
			return null;
		}

		return sessionId;
	}


	@PostMapping("/chat")
	public ResponseEntity<LlmResponse> generateChat(@RequestBody LlmRequest request,
		HttpServletRequest httpServletRequest) {
		ResponseEntity<LlmResponse> responseEntity =
			llmService.generateChat(httpServletRequest, request.getLatitude(), request.getLongitude(), request.getMessage());

		LlmResponse responseBody = responseEntity.getBody();

		if (responseBody == null) {
			return ResponseEntity.status(500)
				.body(new LlmResponse("failed_to_fetch_gpt_response", "GPT 응답이 없습니다."));
		}

		System.out.println("✅ [DEBUG] 최종 응답: " + responseBody.getResponse());

		return ResponseEntity.ok(responseBody);
	}
}
