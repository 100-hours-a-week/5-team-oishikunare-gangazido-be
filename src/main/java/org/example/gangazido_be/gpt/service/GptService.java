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
			headers.set("Authorization", "Bearer " + API_KEY); // 🔥 API 키 추가

			// ✅ OpenAI API 요청 Body 생성
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("model", "gpt-3.5-turbo"); // 🔥 모델 지정
			requestBody.put("messages", List.of(
				Map.of("role", "system", "content", "You are a helpful assistant."),
				Map.of("role", "user", "content", prompt)
			));
			requestBody.put("temperature", 0.7); // 응답 다양성 조절

			HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

			// ✅ OpenAI API 요청 및 응답 받기
			ResponseEntity<String> response = restTemplate.exchange(GPT_API_URL, HttpMethod.POST, requestEntity,
				String.class);

			// ✅ JSON 파싱하여 GPT 응답 추출
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
			List<Map<String, Object>> choices = (List<Map<String, Object>>)responseMap.get("choices");
			String gptResponse = (String)((Map<String, Object>)choices.get(0).get("message")).get("content");

			return gptResponse.trim();
		} catch (Exception e) {
			e.printStackTrace();
			return "GPT 응답을 가져오는 중 오류 발생: " + e.getMessage();
		}
	}
}

