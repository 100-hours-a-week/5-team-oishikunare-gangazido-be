package org.example.gangazido_be.llm.controller;

import java.util.HashMap;
import java.util.Map;

import org.example.gangazido_be.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

	@PostMapping("/chat")
	public ResponseEntity<LlmResponse> generateChat(@RequestBody LlmRequest request,
		HttpServletRequest httpServletRequest, HttpSession session
		) {

		// 세션에서 "user" 객체 가져오기
		User user = (User) session.getAttribute("user");

		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new LlmResponse("not_found_session", "세션에 사용자 정보가 없습니다."));
		}

		Integer sessionUserId = user.getId(); // User 객체에서 userId 추출

		// ✅ 서비스 호출
		ResponseEntity<LlmResponse> responseEntity =
			llmService.generateChat(sessionUserId, httpServletRequest, request.getLatitude(), request.getLongitude(), request.getMessage());

		LlmResponse responseBody = responseEntity.getBody();

		if (responseBody == null) {
			return ResponseEntity.status(500)
				.body(new LlmResponse("failed_to_fetch_gpt_response", "GPT 응답이 없습니다."));
		}

		System.out.println("✅ [DEBUG] 최종 응답: " + responseBody.getResponse());

		return ResponseEntity.ok(responseBody);
	}
}
