package org.example.gangazido_be.llm.controller;
// ✅ LlmController: LLM(GPT)과의 대화 요청을 처리하는 REST 컨트롤러
import java.util.HashMap;
import java.util.Map;

import org.example.gangazido_be.user.entity.User; // ✅ 세션에서 사용자 정보 가져오기 위한 엔티티
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping; // ✅ POST 요청 매핑
import org.springframework.web.bind.annotation.RequestBody; // ✅ 요청 본문(JSON) 매핑
import org.springframework.web.bind.annotation.RequestMapping; // ✅ 세션 접근을 위한 객체
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity; // ✅ 응답 객체
import jakarta.servlet.http.HttpServletRequest; // ✅ 클라이언트 요청 정보
import jakarta.servlet.http.HttpSession; // ✅ 세션 접근을 위한 객체
import org.example.gangazido_be.llm.model.LlmRequest; // ✅ 클라이언트에서 오는 요청 데이터
import org.example.gangazido_be.llm.model.LlmResponse; // ✅ 응답 데이터 포맷
import org.example.gangazido_be.llm.service.LlmService; // ✅ GPT 호출 로직이 있는 서비스


@RestController // ✅ JSON 데이터를 반환하는 REST 컨트롤러임을 명시
@RequestMapping("/v1/llm") // ✅ 이 컨트롤러의 모든 요청은 "/v1/llm" 경로로 시작됨
public class LlmController {
	private final LlmService llmService; // ✅ 의존성 주입받은 LLM 서비스


	public LlmController(LlmService llmService) {
		this.llmService = llmService;
	}

	@PostMapping("")
	public ResponseEntity<LlmResponse> generateChat(@RequestBody LlmRequest request,
		HttpServletRequest httpServletRequest, HttpSession session) {

		// 세션에서 "user" 객체 가져오기
		User user = (User) session.getAttribute("user");
		// ❌ 세션에 사용자 정보가 없으면 401 Unauthorized 응답
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new LlmResponse("not_found_session"));
		}
		// ✅ 세션에 저장된 사용자 객체에서 userId 추출
		Integer sessionUserId = user.getId(); // User 객체에서 userId 추출

		// ✅ GPT 서비스 호출 (유저 ID, 요청, 위도, 경도, 메시지 전달)
		ResponseEntity<LlmResponse> responseEntity =
			llmService.generateChat(sessionUserId, httpServletRequest, request.getLatitude(), request.getLongitude(), request.getMessage());
		// ✅ 응답 본문 추출
		LlmResponse responseBody = responseEntity.getBody();
		// ❌ GPT 응답이 null이면 500 Internal Server Error 응답
		if (responseBody == null) {
			return ResponseEntity.status(500)
				.body(new LlmResponse("failed_to_fetch_gpt_response"));
		}
		// ✅ 정상 응답 로그 출력 (디버깅용)
		System.out.println("✅ [DEBUG] 최종 응답: " + responseBody.getResponse());
		// ✅ 클라이언트에 GPT 응답 전달 (200 OK)
		return ResponseEntity.ok(responseBody);
	}
}
