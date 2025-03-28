// ✅ GPTService: OpenAI GPT API를 호출해서 텍스트를 생성하는 서비스 클래스
package org.example.gangazido_be.gpt.service;

import org.example.gangazido_be.pet.repository.PetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // 외부 api호출을 위한 spring 클래스
import org.springframework.http.*;

import com.fasterxml.jackson.databind.ObjectMapper; // json 처리용 잭슨 라이브러리

import java.util.*;

@Service //이 클래스가 서비스 계층의 빈으로 등록되도록 지정
public class GptService {
// 환경변수에서 오픈 api 키를 가져옴
	private final OpenAiConfig openAiConfig;
	private static final String GPT_API_URL = "https://api.openai.com/v1/chat/completions";
	private final PetRepository petRepository; //  PetRepository 주입 받아 사용 (현재 미사용이지만 필요시 사용 가능)
	// 생성자 주입 방식으로 PetRepository 전달
	public GptService(PetRepository petRepository, OpenAiConfig openAiConfig) {
		this.petRepository = petRepository;
		this.openAiConfig = openAiConfig;
	}
	//  입력 프롬프트를 기반으로 GPT가 생성한 텍스트를 반환하는 메서드
	public String generateText(String prompt) {
		try {
			//  RestTemplate: 외부 HTTP 요청을 보내기 위한 도구
			RestTemplate restTemplate = new RestTemplate();
			String API_KEY = openAiConfig.getApiKey();
			//  HTTP 요청 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON); //요청데이터는 제이슨 형식
			headers.set("Authorization", "Bearer " + API_KEY); //인증을 위한 베어러 토큰 헤더

			// 요청 바디(Map 형태) 구성
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("model", "gpt-3.5-turbo");

			// GPT가 대화 형식이기 때문에 message 리스트 구성
			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(Map.of("role", "system", "content", "You are a helpful assistant."));
			messages.add(Map.of("role", "user", "content", prompt));
			requestBody.put("messages", messages);

			requestBody.put("temperature", 0.7);

			//ObjectMapper를 사용해 requestBody를 JSON 문자열로 변환
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonRequestBody = objectMapper.writeValueAsString(requestBody);
			System.out.println(" [DEBUG] OpenAI API 요청 JSON: " + jsonRequestBody); // 확인용 로그

			// 요청 본문 + 헤더로 HTTP 요청 객체 생성
			HttpEntity<String> requestEntity = new HttpEntity<>(jsonRequestBody, headers);

			// POST 방식으로 GPT API 호출
			ResponseEntity<String> response = restTemplate.exchange(GPT_API_URL, HttpMethod.POST, requestEntity, String.class);
			System.out.println("[DEBUG] OpenAI API 응답 원본: " + response.getBody()); // 원본 응답 확인

			//응답 본문(JSON)을 Map으로 파싱
			Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);

			//응답에서 "choices" 키가 없을 경우 에러 처리
			if (!responseMap.containsKey("choices")) {
				System.err.println("[ERROR] OpenAI 응답에 choices 필드 없음");
				return "GPT 응답이 비어 있습니다.";
			}

			//choices 배열에서 첫 번째 응답 선택
			List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
			if (choices == null || choices.isEmpty()) {
				System.err.println("⚠️ [ERROR] choices 배열이 비어 있음.");
				return "⚠️ GPT 응답이 없습니다.";
			}

			//각 choice 내에 있는 message에서 content 추출
			Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
			if (message == null || !message.containsKey("content")) {
				System.err.println("⚠️ [ERROR] message 필드에 content 없음.");
				return "⚠️ GPT 응답 메시지를 찾을 수 없습니다.";
			}
			System.out.println("[DEBUG] OpenAI API 응답 원본: " + response.getBody()); // 원본 응답 확인
			System.out.println("[DEBUG] OpenAI API 요청 JSON: " + objectMapper.writeValueAsString(requestBody));


			//최종 GPT 응답 문자열 추출 및 반환
			String gptResponse = (String) message.get("content");
			System.out.println("[DEBUG] GPT 최종 응답: " + gptResponse);
			return gptResponse.trim();

		} catch (Exception e) {
			System.err.println("GPT 응답을 처리하는 중 오류 발생: " + e.getMessage());
			e.printStackTrace();
			return "GPT 응답을 가져오는 중 오류 발생: " + e.getMessage();
		}
	}

}
