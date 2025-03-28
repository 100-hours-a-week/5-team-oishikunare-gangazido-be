//llmservice
//String airQualityInfo;
// ✅ LlmService: GPT 기반 반려견 산책 추천 및 대화 생성 서비스
package org.example.gangazido_be.llm.service;

import org.example.gangazido_be.pet.repository.PetRepository;
import org.json.JSONArray;
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
		BREED_CHARACTERISTICS.put("푸들", "곱슬거리는 털이 보온성은 있으나 추위와 더위 모두에 민감해요. 겨울에는 따뜻한 옷을 입히고, 여름엔 시원한 시간대에 산책하세요.");
		BREED_CHARACTERISTICS.put("비숑", "풍성한 털이 있지만 속털이 없어 추위에 약하고 더위에도 약한 편이에요. 겨울엔 방한복, 여름엔 실내 활동을 중심으로 해주세요.");
		BREED_CHARACTERISTICS.put("포메라니안", "이중모를 가졌지만 체구가 작아 추위와 더위 모두에 약해요. 겨울엔 방한복을, 여름엔 외출을 최소화하세요.");
		BREED_CHARACTERISTICS.put("말티즈", "얇고 긴 털을 가지고 있어 추위와 더위 모두에 민감해요. 겨울엔 보온에 신경 쓰고, 여름엔 직사광선을 피하세요.");
		BREED_CHARACTERISTICS.put("웰시코기", "이중모로 추위에는 어느 정도 강하지만 더위에 약해요. 겨울엔 짧은 산책도 가능하지만 여름엔 그늘을 잘 챙겨주세요.");
		BREED_CHARACTERISTICS.put("골든리트리버", "이중모를 가진 대형견으로 추위엔 강하지만 더위엔 쉽게 지칠 수 있어요. 겨울엔 자유로운 활동이 가능하고, 여름엔 휴식을 자주 주세요.");
		BREED_CHARACTERISTICS.put("래브라도리트리버", "추위엔 잘 견디지만 더운 날씨에는 피로를 쉽게 느껴요. 겨울엔 산책하기 좋고, 여름엔 활동 시간을 조절하세요.");
		BREED_CHARACTERISTICS.put("보더콜리", "추위엔 강하지만 더위엔 약한 편이에요. 겨울엔 활발히 움직일 수 있지만, 여름엔 시원한 장소에서 짧은 산책이 좋아요.");
		BREED_CHARACTERISTICS.put("시베리안허스키", "추위엔 매우 강하지만 더위엔 매우 약해요. 겨울엔 야외 활동에 적합하고, 여름엔 실내에서 쉬게 해주세요.");
		BREED_CHARACTERISTICS.put("진돗개", "한국 기후에 잘 적응하지만 혹한과 폭염 모두 주의가 필요해요. 계절에 따라 산책 시간과 환경을 조절하세요.");
		BREED_CHARACTERISTICS.put("믹스견", "혼혈된 특성에 따라 다르지만 보통 기온 변화에 적응력은 있어요. 날씨 변화에 따라 아이의 반응을 관찰해 주세요.");
		BREED_CHARACTERISTICS.put("기타", "견종별로 특성이 다르므로 계절에 맞게 복장과 산책 여부를 조절해 주세요.");
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
				.body(new LlmResponse("invalid_weather_data"));
		}
		//JSONObject json = new JSONObject(response);

		// ✅ 미세먼지 데이터 추출
		JSONObject airQualityJson = weatherJson.optJSONObject("air_quality");
		if (airQualityJson == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new LlmResponse("invalid_air_quality_data"));
		}

		double pm10 = airQualityJson.optDouble("pm10", -1.0);
		double pm25 = airQualityJson.optDouble("pm2_5", -1.0);

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
					"산책 추천 또는 비추천 사유에 기온 %.1f°C, 미세먼지 PM10 %.1fµg/m³, PM2.5 %.1fµg/m³ 수치를 반드시 모두 포함하여 설명하세요**" +
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
				petName, temperature, pm10, pm25, weatherCondition, temperature, pm10, pm25, petName, petBreed, petAge, petWeight
			);
		} else if (lowerMessage.contains("산책") || lowerMessage.contains("산책 가능") || lowerMessage.contains("외출")) {
			prompt = String.format(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					" 날씨와 대기질, 반려견 정보를 바탕으로 **%s**의 산책 가능 여부를 판단하고, JSON 형식으로 추천 결과를 제공해주세요.\n\n" +
					"응답에 반드시 반려견 이름을 포함해주세요." +
					"산책 추천 또는 비추천 사유 (기온 %.1f°C, 미세먼지 PM10 %.1fµg/m³, PM2.5 %.1fµg/m³ 수치를 반드시 모두 포함하여 설명)" +
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
				petName, temperature, pm10, pm25, weatherCondition, temperature, pm10, pm25, petName, petBreed, petAge, petWeight
			);
		} else if (message.contains("옷") || message.contains("입혀야") || lowerMessage.contains("외출 옷") || lowerMessage.contains("방한")) {
			prompt = String.format(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					"반려견이 외출 시 옷을 입어야 할까요? 현재 날씨를 분석하고, 반려견의 체형을 고려하여 적절한 답변을 제공해주세요.\n\n" +
					"응답에 반드시 반려견 이름 %s을 포함해주세요." +
					"산책 추천 또는 비추천 사유 (기온 %.1f°C, 미세먼지 PM10 %.1fµg/m³, PM2.5 %.1fµg/m³ 수치를 반드시 모두 포함하여 설명)" +
					"📌 **현재 환경 데이터:**\n" +
					"- 날씨 상태: %s\n" +
					"- 기온: %.1f°C\n" +
					"- 반려견 견종: %s\n" +
					"- 반려견 체중: %.1fkg\n\n" +
					"📌 **옷을 입어야 하는지 여부와 이유를 한글로 설명해주세요.**" +
					"📌 **응답은 JSON 형식으로 다음과 같이 제공해주세요:**\n" +
					"```json\n" +
					"{\n" +
					"  \"recommendation\": \"산책 추천 또는 비추천\",\n" +
					"  \"reason\": \"산책 추천 또는 비추천 사유\",\n" +
					"  \"safety_tips\": [\"산책 시 유의 사항\"]\n" +
					"}\n" +
					"```\n",

				petName, temperature, pm10, pm25, weatherCondition, temperature, petBreed, petWeight
			);
		} else {
			prompt = "대답할 수 없는 질문이라고 한 문장으로 말해.";
		}

		//System.out.println("📝 [DEBUG] 최종 GPT 프롬프트:\n" + prompt);

		// 🔥 GPT 호출
		String gptResponse;
		try {
			gptResponse = gptService.generateText(prompt);
			System.out.println("response: " + gptResponse);  // 🔍 GPT 응답 확인

			if (gptResponse == null || gptResponse.isEmpty()) {
				throw new Exception("empty_response");
			}
		} catch (Exception e) {
			System.err.println("[ERROR]: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new LlmResponse("failed_to_get_gpt_response"));
		}

		return ResponseEntity.ok(new LlmResponse("llm_success", gptResponse));
		////////////
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

	//  영어 날씨명을 한글로 변환
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

	//  견종명을 한글로 변환
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




