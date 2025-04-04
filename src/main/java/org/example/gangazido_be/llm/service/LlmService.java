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
import java.util.Optional;

// ✅ 이 클래스가 Spring의 Service Bean으로 등록됨
@Service
public class LlmService {
	private final GptService gptService; // ✅ GPT API를 호출하는 서비스
	private final WeatherService weatherService; // ✅ 날씨 데이터를 가져오는 서비스
	private final PetRepository petRepository; // ✅ 반려견 정보를 DB에서 조회하는 Repository


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

		String intentCheckPrompt = String.format(
			"다음 사용자 문장이 어떤 의도를 가지는지 판단해줘.\n" +
				"선택지는 다음과 같아:\n" +
				"- walk_check: 산책 여부 질문\n" +
				"- recommend_route: 산책로 추천\n" +
				"- weather_info: 날씨/공기질 질문\n" +
				"- greeting: 인사, 친근한 말투\n" +
				"- thanks: 감사 표현\n" +
				"- cloth_recommend: 옷 추천 관련 질문\n" +   // ⬅️ 이 줄 추가!
				"- unknown: 알 수 없음\n\n" +
				"아래 예시처럼 판단해줘:\n" +
				"Q: '오늘 어디 갈까?' → recommend_route\n" + // ✅ 이 줄 추가!
				"Q: '산책해도 될까?','오늘 나갈까?' → walk_check\n" +
				"Q: '미세먼지 어때?','오늘 공기 어때?' → weather_info\n" +
				"Q: '옷 입혀야 해?' → cloth_recommend\n" +
				"Q: '안녕!' → greeting\n" +
				"반드시 JSON 형식으로 응답해. 예시: { \"intent\": \"walk_check\" }\n" +
				"문장: \"%s\"\n", message
		);

		String intentResponse;
		String intent;
		try {
			// GPT 호출 먼저
			intentResponse = gptService.generateText(intentCheckPrompt);

			// 안전한 JSON 파싱
			JSONObject intentJson = safeParseJson(intentResponse);
			if (intentJson == null) {
				intent = "unknown";
			} else {
				intent = intentJson.optString("intent", "unknown");
			}
		} catch (Exception e) {
			System.err.println("[ERROR] intent 분석 실패: " + e.getMessage());
			intent = "unknown";
		}

		String prompt = switch (intent) {
			case "weather_info" -> createWeatherPrompt(
				"당신은 반려견 산책 추천 AI입니다. 아래의 조건에 따라 반려견의 산책 가능 여부를 판단해 주세요.\n\n" +
					"📌 반려견 이름은 %s이고, 견종은 %s, 나이는 %d살, 몸무게는 %.1fkg입니다.\n" +
					"📌 현재 날씨는 %s이고, 기온은 %.1f°C입니다.\n" +
					"📌 미세먼지 상태:\n" +
					"- PM10: %.1f µg/m³\n" +
					"- PM2.5: %.1f µg/m³\n\n" +
					"미세먼지 상태에 따라 '좋음', '보통', '나쁨' 수준으로 분류하고, 반려견의 건강에 미치는 영향과 함께 산책을 추천할지 여부를 판단해주세요.\n\n" +
					"⚠️ 만약 미세먼지 수치가 '나쁨' 수준이면 외출을 자제하라고 안내하고, 실내 활동을 추천해 주세요.\n" +
					"✅ 응답은 반드시 아래 JSON 형식으로만 제공해주세요:\n\n" +
					"{\n" +
					"  \"recommendation\": \"미세먼지 상태(좋음/보통/나쁨)\",\n" +
					"  \"reason\": \"정확한 미세먼지 수치 기반의 판단 및 설명\",\n" +
					"  \"safety_tips\": [\"산책 시 유의 사항 또는 실내 활동 팁\"]\n" +
					"}",
				petName, petBreed, petAge, petWeight,
				weatherCondition, temperature, pm10, pm25);
			case "walk_check" -> createWalkCheckPrompt(
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
					" json\n" +
					"{\n" +
					"  \"recommendation\": \"산책 추천 또는 비추천\",\n" +
					"  \"reason\": \"산책 추천 또는 비추천 사유\",\n" +
					"  \"safety_tips\": [\"산책 시 유의 사항\"]\n" +
					"}\n" +
					"\n",
				petName, temperature, pm10, pm25, weatherCondition, temperature, pm10, pm25, petName, petBreed, petAge,
				petWeight);
			case "recommend_route" -> createRouteRecommendPrompt(
				"당신은 위치 기반 산책로 추천 AI입니다.\n" +
					"현재 위치는 위도 %.6f, 경도 %.6f 입니다.\n" +
					"이 근처 반경 2~3km 이내의 산책 가능한 장소 3곳을 추천해주세요.\n" +
					"**아래 JSON 형식으로만 응답하세요.**\n\n" +
					"{\n" +
					"  \"routes\": [\n" +
					"    { \"name\": \"산책로 이름\", \"description\": \"특징 및 반려견과의 산책 추천 이유\", \"distance_km\": 거리 }\n" +
					"  ]\n" +
					"}",
				latitude, longitude);
			case "greeting" -> createFriendlyPrompt(
				"너는 귀여운 반려견 산책 도우미 AI야. 사용자와 친근한 말투로 짧게 대답해줘.\n" +
					"반려견 이름은 %s이고, 견종은 %s야. 사용자가 '%s'라고 말했어. 너무 딱딱하지 않게, 공감하며 답장해줘. 문장은 한두 문장으로 간단히 해줘.\n\n" +
					"예시)\n" +
					"Q: 안녕\nA: 안녕하세요! 🐶 반가워요!\n\n" +
					"Q: 고마워\nA: 도움이 됐다니 저도 기뻐요!\n\n" +
					"Q: 산책 어디로 갈까?\nA: 날씨 좋을 땐 가까운 공원도 좋겠네요!\n\n" +
					"Q: %s\nA:",
				petName, petBreed, message, message);
			case "cloth_recommend" -> clothRecommendPrompt(
				"당신은 반려견 산책 추천 AI입니다. **반드시 JSON 형식으로만 답변하세요.** HTML이나 마크다운, 자연어 문장만 있는 응답은 허용되지 않습니다.\\n" +
					"반려견이 외출 시 옷을 입어야 할까요? 현재 날씨를 분석하고, 반려견의 특징을 고려하여 적절한 답변을 제공해주세요.\n\n" +
					"응답에 반드시 반려견 이름 %s을 포함해주세요." +
					"옷입히기 추천 또는 비추천 사유 (기온 %.1f°C, 미세먼지 PM10 %.1fµg/m³, PM2.5 %.1fµg/m³ 수치를 반드시 모두 포함하여 설명)" +
					"📌 **현재 환경 데이터:**\n" +
					"- 날씨 상태: %s\n" +
					"- 기온: %.1f°C\n" +
					"- 반려견 견종: %s\n" +
					"- 반려견 체중: %.1fkg\n\n" +
					"📌 **옷을 입어야 하는지 여부와 이유를 한글로 설명해주세요.**" +
					"📌 **응답은 JSON 형식으로 다음과 같이 제공해주세요:**\n" +
					" json\n" +
					"{\n" +
					"  \"recommendation\": \"옷 입히기 추천 또는 비추천\",\n" +
					"  \"reason\": \"옷 입히기 추천 또는 비추천 사유\",\n" +
					"  \"safety_tips\": [\"산책 시 유의 사항\"]\n" +
					"}\n" +
					"\n",
				petName, temperature, pm10, pm25, weatherCondition, temperature, petBreed, petWeight);
			default -> "대답할 수 없는 질문이라고 한 문장으로 말해.";
		};

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

	}

	private JSONObject safeParseJson(String raw) {
		try {
			// 백틱 제거
			if (raw.startsWith("```")) {
				raw = raw.replaceAll("```[a-z]*", "").trim();
			}

			// JSON 블록 추출
			int start = raw.indexOf("{");
			int end = raw.lastIndexOf("}");
			if (start != -1 && end != -1 && start < end) {
				raw = raw.substring(start, end + 1);
			}

			// 실제 파싱
			return new JSONObject(raw);
		} catch (Exception e) {
			System.err.println("[ERROR] GPT 응답 JSON 파싱 실패: " + e.getMessage());
			return null;
		}
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

	private String createWeatherPrompt(String template, String petName, String petBreed, int petAge,
		double petWeight, String weatherCondition, double temperature,
		double pm10, double pm25) {
		return String.format(template, petName, petBreed, petAge, petWeight, weatherCondition,
			temperature, pm10, pm25);
	}

	private String createWalkCheckPrompt(String template, String petName, double temperature, double pm10,
		double pm25, String weatherCondition, double temp, double pm10_2,
		double pm25_2, String name, String breed, int age, double weight) {
		return String.format(template, petName, temperature, pm10, pm25, weatherCondition, temp,
			pm10_2, pm25_2, name, breed, age, weight);
	}

	private String createRouteRecommendPrompt(String template, double latitude, double longitude) {
		return String.format(template, latitude, longitude);
	}

	private String createFriendlyPrompt(String template, String petName, String breed, String message, String userMessage) {
		return String.format(template, petName, breed, message, userMessage);
	}

	private String clothRecommendPrompt(String template, String petName, double temperature,
		double pm10, double pm25, String weatherCondition, double temp, String breed, double weight) {
		return String.format(template, petName, temperature, pm10, pm25, weatherCondition, temp, breed, weight);
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
