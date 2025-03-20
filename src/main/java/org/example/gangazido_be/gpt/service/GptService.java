//GPTService
package org.example.gangazido_be.gpt.service;

import org.springframework.stereotype.Service;
import org.example.gangazido_be.gpt.model.PetRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class GptService {

	private static final String API_KEY = System.getenv("OPENAI_API_KEY"); // 🔥 OpenAI API 키
	private static final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";
	private final PetRepository petRepository;

	public GptService(PetRepository petRepository) {
		this.petRepository = petRepository;
	}

	public String generateText(String prompt) {
		try {
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", "Bearer " + API_KEY);

			// ✅ OpenAI API 요청 Body 생성
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("model", "gpt-3.5-turbo");

			// ✅ "messages" 배열 생성 (List.of() 대신 new ArrayList<>() 사용)
			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(Map.of("role", "system", "content", "You are a helpful assistant."));
			messages.add(Map.of("role", "user", "content", prompt));
			requestBody.put("messages", messages);

			requestBody.put("temperature", 0.7);

			// ✅ JSON 변환 문제 해결을 위해 `ObjectMapper` 사용
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonRequestBody = objectMapper.writeValueAsString(requestBody);
			System.out.println("📩 [DEBUG] OpenAI API 요청 JSON: " + jsonRequestBody); // 확인용 로그

			HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, headers);

			// ✅ OpenAI API 요청 및 응답 받기
			ResponseEntity<String> response = restTemplate.exchange(GPT_API_URL, HttpMethod.POST, requestEntity, String.class);
			System.out.println("📩 [DEBUG] OpenAI API 응답 원본: " + response.getBody()); // 원본 응답 확인

			// ✅ JSON 파싱하여 GPT 응답 추출
			Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

			// 🚨 "choices" 필드 확인
			if (!responseMap.containsKey("choices")) {
				System.err.println("⚠️ [ERROR] OpenAI 응답에 choices 필드 없음");
				return "⚠️ GPT 응답이 비어 있습니다.";
			}

			List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
			if (choices == null || choices.isEmpty()) {
				System.err.println("⚠️ [ERROR] choices 배열이 비어 있음.");
				return "⚠️ GPT 응답이 없습니다.";
			}

			// 🚨 "message" 필드 확인
			Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
			if (message == null || !message.containsKey("content")) {
				System.err.println("⚠️ [ERROR] message 필드에 content 없음.");
				return "⚠️ GPT 응답 메시지를 찾을 수 없습니다.";
			}
			System.out.println("📩 [DEBUG] OpenAI API 응답 원본: " + response.getBody()); // 원본 응답 확인
			System.out.println("📤 [DEBUG] OpenAI API 요청 JSON: " + objectMapper.writeValueAsString(requestBody));



			String gptResponse = (String) message.get("content");
			System.out.println("✅ [DEBUG] GPT 최종 응답: " + gptResponse);
			return gptResponse.trim();
		} catch (Exception e) {
			System.err.println("❌ GPT 응답을 처리하는 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
			return "GPT 응답을 가져오는 중 오류 발생: " + e.getMessage();
		}
	}

}
