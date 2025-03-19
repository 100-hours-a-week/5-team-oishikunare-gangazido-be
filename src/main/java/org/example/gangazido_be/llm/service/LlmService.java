//llmservice

package org.example.gangazido_be.llm.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.http.HttpStatus;
import org.example.gangazido_be.llm.model.LlmResponse;
import org.example.gangazido_be.gpt.service.GptService;
import org.example.gangazido_be.gpt.service.WeatherService;
import org.example.gangazido_be.gpt.model.Pet;
import org.example.gangazido_be.gpt.model.PetRepository;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class LlmService {
	private final GptService gptService;
	private final WeatherService weatherService;
	private final PetRepository petRepository;
	private final Map<String, LlmResponse> responseCache = new HashMap<>();

	public LlmService(GptService gptService, WeatherService weatherService, PetRepository petRepository) {
		this.gptService = gptService;
		this.weatherService = weatherService;
		this.petRepository = petRepository;
	}

	@SuppressWarnings("checkstyle:OperatorWrap")
	public LlmResponse generateChat(HttpServletRequest request, double latitude, double longitude, String message) {
		String sessionId = extractSessionId(request);
		int userId;
		try {
			userId = (sessionId != null) ? Integer.parseInt(sessionId) : 2;
		} catch (NumberFormatException e) {
			System.err.println("⚠️ [경고] 세션 ID가 잘못되었습니다. 기본값(2) 사용.");
			userId = 2; // 기본값 사용
		}

		List<Pet> pets = petRepository.findByUserId(userId);
		try {
			pets = petRepository.findByUserId(userId);
		} catch (Exception e) {
			System.err.println("❌ [에러] 반려견 정보를 조회하는 중 오류 발생: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_retrieve_pet_info", "❌ 반려견 정보를 가져오는 중 오류가 발생했습니다."));
		}

		if (pets.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new LlmResponse("not_found_pet", "❌ 반려견 정보를 찾을 수 없습니다."));
		}

		Pet pet = pets.get(0);

		String weatherInfo;
		try {
			weatherInfo = weatherService.getWeather(latitude, longitude);
			if (weatherInfo == null || weatherInfo.isEmpty()) {
				throw new Exception("Weather API returned an empty response.");
			}
		} catch (Exception e) {
			System.err.println("❌ [에러] 날씨 정보를 가져오는 중 오류 발생: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_fetch_weather", "❌ 날씨 정보를 가져오는 중 오류가 발생했습니다."));

		}

		JSONObject weatherJson;
		try {
			weatherJson = new JSONObject(weatherInfo);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new LlmResponse("invalid_weather_json", "❌ 날씨 데이터 처리 중 오류가 발생했습니다."));
		}

		// ✅ JSON에서 필요한 데이터 추출 (예외 처리 포함)
		JSONObject weatherData = weatherJson.optJSONObject("weather");
		JSONObject airQualityData = weatherJson.optJSONObject("air_quality");

		String weatherCondition = (weatherData != null && weatherData.has("condition")) ? convertWeatherToKorean(weatherData.getString("condition")) : "알 수 없음";
		double temperature = (weatherData != null) ? weatherData.optDouble("temperature", 0.0) : 0.0;
		double pm10 = (airQualityData != null) ? airQualityData.optDouble("pm10", -1.0) : -1.0;
		double pm25 = (airQualityData != null) ? airQualityData.optDouble("pm2_5", -1.0) : -1.0;

		String petName = pet.getName();
		String petBreed = convertBreedToKorean(pet.getBreed());
		int petAge = pet.getAge();
		double petWeight = pet.getWeight();

		System.out.println("🌤️ [날씨 상태]: " + weatherCondition);
		System.out.println("🌡️ [기온]: " + temperature);
		System.out.println("💨 [미세먼지 PM10]: " + pm10);
		System.out.println("💨 [초미세먼지 PM2.5]: " + pm25);

		// 📌 **질문 유형에 따라 다른 프롬프트 생성**
		String prompt;
		String lowerMessage = message.toLowerCase();
		if (lowerMessage.contains("미세먼지") || lowerMessage.contains("공기질") || lowerMessage.contains("대기") || lowerMessage.contains("미먼")) {
			prompt = String.format(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					"미세먼지 데이터와 반려견 정보를 바탕으로 **%s**의 산책 가능 여부를 판단하고 그 결과를 제공해주세요" +
					"📌 **현재 환경 데이터:**\n" +
					"- 날씨 상태: %s\n" +
					"- 기온: %.1f°C\n" +
					"- 미세먼지(PM10): %.1f µg/m³\n" +
					"- 초미세먼지(PM2.5): %.1f µg/m³\n" +
					"- 반려견 정보:\n" +
					"  - 이름: %s\n" +
					"  - 견종: %s\n" +
					"  - 나이: %d살\n" +
					"  - 무게: %.1fkg\n\n" +
					"📌 **미세먼지가 반려견 산책에 미치는 영향을 고려하여 JSON 형식으로 답변해주세요:**\n" +
					"```json\n" +
					"{\n" +
					"  \"recommendation\": \"산책 추천 또는 비추천\",\n" +
					"  \"reason\": \"산책 추천 또는 비추천 사유 (미세먼지 영향 포함)\",\n" +
					"  \"safety_tips\": [\"산책 시 유의 사항\"]\n" +
					"}\n" +
					"```\n" +
					"**반드시 위 JSON 형식을 지켜서 응답하세요.**",
				petName, weatherCondition, temperature, pm10, pm25, petName, petBreed, petAge, petWeight
			);
		} else if (lowerMessage.contains("산책") || lowerMessage.contains("산책 가능") || lowerMessage.contains("외출")) {
			prompt = String.format(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					" 날씨와 대기질, 반려견 정보를 바탕으로 **%s**의 산책 가능 여부를 판단하고, JSON 형식으로 추천 결과를 제공해주세요.\n\n" +
					"📌 **현재 환경 데이터:**\n" +
					"- 날씨 상태: %s\n" +
					"- 기온: %.1f°C\n" +
					"- 미세먼지(PM10): %.1f µg/m³\n" +
					"- 초미세먼지(PM2.5): %.1f µg/m³\n" +
					"- 반려견 정보:\n" +
					"  - 이름: %s\n" +
					"  - 견종: %s\n" +
					"  - 나이: %d살\n" +
					"  - 무게: %.1fkg\n\n" +
					"📌 **응답은 JSON 형식으로 다음과 같이 제공해주세요:**\n" +
					"```json\n" +
					"{\n" +
					"  \"recommendation\": \"산책 추천 또는 비추천\",\n" +
					"  \"reason\": \"산책 추천 또는 비추천 사유\",\n" +
					"  \"safety_tips\": [\"산책 시 유의 사항\"]\n" +
					"}\n" +
					"```\n",
				petName, weatherCondition, temperature, pm10, pm25, petName, petBreed, petAge, petWeight
			);
		} else if (message.contains("옷") || message.contains("입어야") || lowerMessage.contains("외출 옷") || lowerMessage.contains("방한")) {
			prompt = String.format(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					"날씨 데이터와 반려견 정보를 바탕으로 **%s**의 산책 가능 여부를 판단하고 그 결과를 제공해주세요" +
					"반려견이 외출 시 옷을 입어야 할까요? 현재 날씨를 분석하고, 반려견의 체형을 고려하여 적절한 답변을 제공해주세요.\n\n" +
					"📌 **현재 환경 데이터:**\n" +
					"- 날씨 상태: %s\n" +
					"- 기온: %.1f°C\n" +
					"- 반려견 견종: %s\n" +
					"- 반려견 체중: %.1fkg\n\n" +
					"📌 **옷을 입어야 하는지 여부와 이유를 한글로 설명해주세요.**",
				weatherCondition, temperature, petBreed, petWeight
			);
		} else {
			prompt = "제가 도와드릴 수 있는 질문이 아닙니다.";
		}

		System.out.println("📝 [DEBUG] 최종 GPT 프롬프트:\n" + prompt);

		// 🔥 GPT 호출
		String gptResponse;

		try {
			gptResponse = gptService.generateText(prompt);
			if (gptResponse == null || gptResponse.isEmpty()) {
				throw new Exception("GPT service returned an invalid or empty response.");
			}
		} catch (Exception e) {
			System.err.println("❌ [에러] GPT 응답 오류: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_fetch_gpt_response", "❌ AI 응답을 가져오는 중 오류가 발생했습니다."));
		}

		return new LlmResponse("llm_success", gptResponse);
	}

	private String extractSessionId(HttpServletRequest request) {
		String cookie = request.getHeader("Cookie");
		if (cookie == null || !cookie.contains("connect.sid=")) {
			return null;
		}

		String[] parts = cookie.split("connect.sid=");
		if (parts.length < 2) {
			return null;
		}

		return parts[1].split(";")[0].trim();
	}

	private String convertWeatherToKorean(String weather) {
		switch (weather.toLowerCase()) {
			case "clear":
			case "sunny":
				return "맑음";
			case "clouds":
			case "cloudy":
				return "흐림";
			case "rain":
			case "drizzle":
				return "비";
			case "snow":
				return "눈";
			case "fog":
			case "haze":
			case "mist":
				return "안개";
			case "storm":
			case "thunderstorm":
				return "폭풍";
			default:
				return weather; // 변환할 수 없는 경우 원래 값 유지
		}
	}

	private String convertBreedToKorean(String breed) {
		switch (breed.toLowerCase()) {
			case "poodle":
				return "푸들";
			case "bichon":
				return "비숑 프리제";
			case "pomeranian":
				return "포메라니안";
			case "maltese":
				return "말티즈";
			case "welshcorgi":
				return "웰시코기";
			case "goldenretriever":
				return "골든 리트리버";
			case "labradorretriever":
				return "래브라도 리트리버";
			case "bordercollie":
				return "보더 콜리";
			case "siberianhusky":
				return "시베리안 허스키";
			case "jindodog":
				return "진돗개";
			case "mixedbreed":
				return "믹스견";
			case "others":
				return "기타";
			default:
				return breed; // 변환할 수 없는 경우 원래 값 유지
		}
	}

}




