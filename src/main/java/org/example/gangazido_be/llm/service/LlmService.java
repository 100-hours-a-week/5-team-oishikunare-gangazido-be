//llmservice
//String airQualityInfo;
// ✅ LlmService: GPT 기반 반려견 산책 추천 및 대화 생성 서비스
package org.example.gangazido_be.llm.service;

import org.example.gangazido_be.pet.repository.PetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.example.gangazido_be.llm.model.LlmResponse;
import org.example.gangazido_be.gpt.service.GptService;
import org.example.gangazido_be.gpt.service.WeatherService;
import org.example.gangazido_be.pet.entity.Pet;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

// ✅ 이 클래스가 Spring의 Service Bean으로 등록됨
@Service
public class LlmService {
	private final GptService gptService; // ✅ GPT API를 호출하는 서비스
	private final WeatherService weatherService; // ✅ 날씨 데이터를 가져오는 서비스
	private final PetRepository petRepository; // ✅ 반려견 정보를 DB에서 조회하는 Repository
	// ✅ 캐시를 사용하여 GPT 응답을 저장하여 성능 최적화
	private final Map<String, LlmResponse> responseCache = new HashMap<>();

	// ✅ 견종별 특성을 정의 (추위에 대한 내성)
	private static final Map<String, String> BREED_CHARACTERISTICS = new HashMap<>();

	static {
		BREED_CHARACTERISTICS.put("siberian husky", "추운 날씨에서 활동하기 적합한 견종입니다.");
		BREED_CHARACTERISTICS.put("golden retriever", "추위에 비교적 강하지만, 너무 추운 날씨에는 보호가 필요할 수 있습니다.");
		BREED_CHARACTERISTICS.put("pomeranian", "추위에 약하므로 따뜻한 옷을 입히는 것이 좋습니다.");
		BREED_CHARACTERISTICS.put("maltese", "추위에 약한 견종이므로 외출 시 방한복이 필요합니다.");
		BREED_CHARACTERISTICS.put("bichon", "포근한 털이 있지만 추위에 약한 편이라 옷을 입히는 것이 좋아요.");
		BREED_CHARACTERISTICS.put("jindodog", "적당한 기온에서는 산책이 가능하지만, 너무 추운 날씨에는 주의해야 합니다.");
		BREED_CHARACTERISTICS.put("mixedbreed", "견종에 따라 차이가 있지만 일반적으로 기온 변화에 적응할 수 있습니다.");
		BREED_CHARACTERISTICS.put("others", "견종별 특성을 고려하여 산책 여부를 결정하세요.");
	}

	// ✅ 생성자 주입 방식으로 의존성 주입 (Spring이 자동으로 관리)
	public LlmService(GptService gptService, WeatherService weatherService, PetRepository petRepository) {
		this.gptService = gptService;
		this.weatherService = weatherService;
		this.petRepository = petRepository;
	}

	/**
	 * ✅ GPT 기반 반려견 산책 추천 API
	 * - 세션에서 사용자 ID를 가져와 반려견 정보를 조회
	 * - OpenWeather API를 호출하여 날씨 및 공기질 정보를 가져옴
	 * - 반려견 정보 + 날씨 데이터를 조합하여 GPT에 질문을 보내 응답을 생성
	 */
	//세션 id 받아오기
	@SuppressWarnings("checkstyle:OperatorWrap")
	public ResponseEntity<LlmResponse> generateChat(Integer sessionUserId, HttpServletRequest request, double latitude,
		double longitude, String message) {

		// 🐶 반려견 정보 조회
		Optional<Pet> pets;
		try {
			pets = petRepository.findByUserId(sessionUserId);
			if (pets.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new LlmResponse("not_found_pet", "반려견 정보를 찾을 수 없습니다."));
			}
		} catch (Exception e) {
			System.err.println("[ERROR] failed_to_get_pet_info: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_get_pet_info", "서버 오류로 인해 반려견 정보를 가져올 수 없습니다."));
		}

		Pet pet = pets.get();

		// 🌤️ 날씨 정보 가져오기
		String weatherInfo;
		try {
			weatherInfo = weatherService.getWeather(latitude, longitude);
			if (weatherInfo == null || weatherInfo.isEmpty()) {
				System.err.println("[ERROR] failed_to_get_weather: 날씨 정보 응답이 비어 있음");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new LlmResponse("failed_to_get_weather", "날씨 정보를 가져오지 못했습니다."));
			}
		} catch (Exception e) {
			System.err.println("[ERROR] failed_to_get_weather: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_get_weather", "서버 오류로 인해 날씨 정보를 가져올 수 없습니다."));
		}

		// ✅ 날씨 JSON 데이터 파싱
		JSONObject weatherJson;
		try {
			weatherJson = new JSONObject(weatherInfo);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new LlmResponse("invalid_weather_data", "internal_server_error"));
		}
		//JSONObject json = new JSONObject(response);

		// ✅ 미세먼지 데이터 추출
		JSONObject airQualityJson = weatherJson.optJSONObject("air_quality");
		if (airQualityJson == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new LlmResponse("invalid_air_quality_data", "internal_server_error"));
		}

		JSONObject components = airQualityJson.optJSONObject("components");
		double pm10 = (components != null) ? components.optDouble("pm10", -1.0) : -1.0;
		double pm25 = (components != null) ? components.optDouble("pm2_5", -1.0) : -1.0;

		// ✅ 날씨 정보 파싱
		JSONObject weatherData = weatherJson.optJSONObject("weather");
		String weatherCondition = (weatherData != null && weatherData.has("condition")) ?
			convertWeatherToKorean(weatherData.getString("condition")) : "알 수 없음";

		double temperature = (weatherData != null) ? weatherData.optDouble("temperature", 0.0) : 0.0;

		// ✅ 반려견 정보
		String petName = pet.getName();
		String petBreed = convertBreedToKorean(pet.getBreed());
		int petAge = pet.getAge();
		double petWeight = pet.getWeight();

		System.out.println("🌤️ [날씨 상태]: " + weatherCondition);
		System.out.println("🌡️ [기온]: " + temperature);
		System.out.println("💨 [미세먼지 PM10]: " + pm10);
		System.out.println("💨 [초미세먼지 PM2.5]: " + pm25);

		// ✅ GPT 프롬프트 생성 (산책 가능 여부)
		String prompt;
		String lowerMessage = message.toLowerCase();
		if (lowerMessage.contains("미세먼지") || lowerMessage.contains("공기") || lowerMessage.contains("대기") || lowerMessage.contains("날씨")) {
			prompt = String.format(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					"미세먼지 데이터와 반려견 정보를 바탕으로 **%s**의 산책 가능 여부를 판단하고 그 결과를 제공해주세요" +
					"응답에 반드시 반려견 이름을 포함해주세요." +
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
					"응답에 반드시 반려견 이름을 포함해주세요." +
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
					"응답에 반드시 반려견 이름을 포함해주세요." +
					"📌 **현재 환경 데이터:**\n" +
					"- 날씨 상태: %s\n" +
					"- 기온: %.1f°C\n" +
					"- 반려견 견종: %s\n" +
					"- 반려견 체중: %.1fkg\n\n" +
					"📌 **옷을 입어야 하는지 여부와 이유를 한글로 설명해주세요.**",
				weatherCondition, temperature, petBreed, petWeight
			);
		} else {
			prompt = "'제가 도와드릴 수 있는 질문이 아닙니다'라고 답해.";
		}

		System.out.println("📝 [DEBUG] 최종 GPT 프롬프트:\n" + prompt);

		// 🔥 GPT 호출
		String gptResponse;
		try {
			gptResponse = gptService.generateText(prompt);
			System.out.println("response: " + gptResponse);  // 🔍 GPT 응답 확인
			if (gptResponse == null || gptResponse.isEmpty()) {
				throw new Exception("GPT 서비스에서 응답이 비어 있습니다.");
			}
		} catch (Exception e) {
			System.err.println("[ERROR]: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_get_gpt_response", "failed_to_get_gpt_response"));
		}

		return ResponseEntity.ok(new LlmResponse("llm_success", gptResponse));
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

	// ✅ 영어 날씨명을 한글로 변환
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

	// ✅ 견종명을 한글로 변환
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




