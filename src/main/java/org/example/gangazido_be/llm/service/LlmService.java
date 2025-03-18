//llmservice

package org.example.gangazido_be.llm.service;

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
	public LlmResponse generateChat(HttpServletRequest request, double latitude, double longitude) {
		String sessionId = extractSessionId(request);
		int userId;
		try {
			userId = (sessionId != null) ? Integer.parseInt(sessionId) : 2;
		} catch (NumberFormatException e) {
			userId = 2; // 기본값 사용
		}

		List<Pet> pets = petRepository.findByUserId(userId);
		if (pets.isEmpty()) {
			return new LlmResponse("no_pet_found", "❌ 반려견 정보를 찾을 수 없습니다.");
		}

		Pet pet = pets.get(0);

		String weatherInfo = weatherService.getWeather(latitude, longitude);
		if (weatherInfo == null || weatherInfo.isEmpty()) {
			return new LlmResponse("weather_fetch_error", "날씨 정보를 가져오는 중 오류가 발생했습니다.");
		}

		JSONObject weatherJson = new JSONObject(weatherInfo);
		// ✅ JSON 객체로 바로 가져오기
		JSONObject weatherData = weatherJson.getJSONObject("weather");

		JSONObject airQualityData = weatherJson.getJSONObject("air_quality");

		String weatherCondition = weatherData.getString("condition");
		double temperature = weatherData.getDouble("temperature"); // ✅ Double 타입 유지
		double pm10 = airQualityData.getDouble("pm10");
		double pm2_5 = airQualityData.getDouble("pm2_5");

		String petName = (pet.getName() != null) ? pet.getName() : "이름 없음";
		String petBreed = (pet.getBreed() != null) ? pet.getBreed() : "견종 정보 없음";
		int petAge = (pet.getAge() > 0) ? pet.getAge() : 1;
		double petWeight = (pet.getWeight() > 0) ? pet.getWeight() : 5.0;
		// 🌟 날씨 & 미세먼지 정보 직접 출력
		System.out.println("🌤️ [현재 날씨 정보]");
		System.out.println("- 날씨 상태: " + weatherCondition);
		System.out.println("- 기온: " + String.format("%.1f°C", temperature));
		System.out.println("- 미세먼지(PM10): " + String.format("%.1f µg/m³", pm10));
		System.out.println("- 초미세먼지(PM2.5): " + String.format("%.1f µg/m³", pm2_5));

		System.out.println("\n🐶 [반려견 정보]");
		System.out.println("- 이름: " + petName);
		System.out.println("- 견종: " + petBreed);
		System.out.println("- 나이: " + petAge + "살");
		System.out.println("- 무게: " + String.format("%.1fkg", petWeight));

		System.out.println("\n📢 [산책 추천 결과]\n");

		String prompt = String.format(
			"당신은 반려견 산책 추천 AI입니다. 아래 데이터를 참고하여 반려견이 안전하게 산책할 수 있는지 판단하고, " +
				"**반드시 반려견의 이름을 포함하여** 산책 추천 여부와 이유, 안전 팁을 JSON 형식으로 응답하세요.\n\n" +
				"**응답 시 반드시 한글로 작성해야 합니다! 영어 단어는 사용하지 마세요.**" +
				"📌 **주어진 환경 데이터:**\n" +
				"- 현재 날씨 상태: %s\n" +
				"- 현재 기온: %.1f°C\n" +
				"- 미세먼지(PM10): %.1f µg/m³\n" +
				"- 초미세먼지(PM2.5): %.1f µg/m³\n" +
				"- 반려견 정보:\n" +
				"  - 이름: %s\n" +
				"  - 견종: %s\n" +
				"  - 나이: %d살\n" +
				"  - 무게: %.1fkg\n\n" +
				"📌 **반려견의 이름(%s)을 반드시 포함하여 응답해야 합니다!**\n" +
				"예를 들어, 반려견 이름이 '%s'이면 다음과 같이 응답하세요:\n\n" +
				"```json\n" +
				"{\n" +
				"    \"recommendation\": \"산책 추천\",\n" +
				"    \"reason\": \"날씨 상태가 %s이고 기온이 %.1f°C로 %s. 미세먼지 수치도 %s. %s에게 딱 알맞은 산책 환경이군요!\",\n" +
				"    \"safety_tips\": [\n" +
				"        \"%s는 %s이므로, 산책 후 충분한 휴식과 물을 제공해야 합니다.\",\n" +
				"        \"%d살이라는 나이는 %s 시기이므로, 산책 중에는 반려견의 에너지를 잘 조절하도록 해주세요.\",\n" +
				"        \"산책 도중 갑자기 뛰어다니는 등의 행동에 주의하여 안전을 유지해주세요.\"\n" +
				"    ]\n" +
				"}\n" +
				"```\n\n",

			// ✅ `String.format()`에서 올바른 타입을 사용하도록 수정
			weatherCondition, temperature, pm10, pm2_5,
			petName, petBreed, petAge, petWeight,

			petName, petName,

			weatherCondition, temperature,
			(temperature < 10) ? "쌀쌀합니다" : "적당합니다",
			(pm10 < 50 && pm2_5 < 25) ? "좋습니다" : "조금 높습니다",
			petName,

			petName, petBreed, petAge,
			(petAge <= 2) ? "활발한 활동을 즐기는" : "적당한 운동이 필요한"
		);

		String gptResponse = gptService.generateText(prompt);
		LlmResponse response = new LlmResponse("llm_success", gptResponse);
		responseCache.put(sessionId, response);

		return response;
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
				return "맑음";
			case "cloudy":
				return "흐림";
			case "rain":
				return "비";
			case "snow":
				return "눈";
			case "fog":
				return "안개";
			case "storm":
				return "폭풍";
			default:
				return weather; // 변환할 수 없는 경우 원래 값 유지
		}
	}

	private String convertBreedToKorean(String breed) {
		switch (breed.toLowerCase()) {
			case "pomeranian":
				return "포메라니안";
			case "golden retriever":
				return "골든 리트리버";
			case "bulldog":
				return "불독";
			case "shiba inu":
				return "시바 이누";
			case "border collie":
				return "보더 콜리";
			case "labrador retriever":
				return "래브라도 리트리버";
			case "beagle":
				return "비글";
			case "siberian husky":
				return "시베리안 허스키";
			case "german shepherd":
				return "저먼 셰퍼드";
			case "chihuahua":
				return "치와와";
			default:
				return breed; // 변환할 수 없는 경우 원래 값 유지
		}
	}

}



