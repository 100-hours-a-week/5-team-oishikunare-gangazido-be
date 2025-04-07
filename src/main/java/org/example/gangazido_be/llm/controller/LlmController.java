package org.example.gangazido_be.llm.controller;

import org.example.gangazido_be.user.entity.User; // 세션에서 사용자 정보 가져오기 위한 엔티티
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping; //  POST 요청 매핑
import org.springframework.web.bind.annotation.RequestBody; //  요청 본문(JSON) 매핑
import org.springframework.web.bind.annotation.RequestMapping; //  세션 접근을 위한 객체
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity; //  응답 객체
import jakarta.servlet.http.HttpServletRequest; //  클라이언트 요청 정보
import jakarta.servlet.http.HttpSession; //  세션 접근을 위한 객체
import org.example.gangazido_be.llm.model.LlmRequest; // 클라이언트에서 오는 요청 데이터
import org.example.gangazido_be.llm.model.LlmResponse; //  응답 데이터 포맷
import org.example.gangazido_be.llm.service.LlmService; //  GPT 호출 로직이 있는 서비스


@RestController // JSON 데이터를 반환하는 REST 컨트롤러임을 명시
@RequestMapping("/v1/llm") //  이 컨트롤러의 모든 요청은 "/v1/llm" 경로로 시작됨
public class LlmController {
	private final LlmService llmService; //  의존성 주입받은 LLM 서비스
	private Object responseEntity;

	public LlmController(LlmService llmService) {
		this.llmService = llmService;
	}

	@PostMapping("")
	public ResponseEntity<LlmResponse> generateChat(@RequestBody LlmRequest request,
		HttpServletRequest httpServletRequest, HttpSession session) {

		User user = (User)session.getAttribute("user");
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new LlmResponse("not_found_session"));
		}

		LlmResponse response = llmService.generateChat(
			user.getId(), httpServletRequest,
			request.getLatitude(), request.getLongitude(), request.getMessage()).getBody();

		return llmService.generateChat(
			user.getId(), httpServletRequest,
			request.getLatitude(), request.getLongitude(), request.getMessage());
	}
}
